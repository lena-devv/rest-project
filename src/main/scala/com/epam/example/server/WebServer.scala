package com.epam.example.server

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.{Forbidden, InternalServerError}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.epam.example.client.DomainModel.MessageEvent
import spray.json.DefaultJsonProtocol.jsonFormat4

import scala.concurrent.Future
import scala.io.StdIn

import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.server._

object WebServer extends JsonSupport {
  // needed to run the route
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext = system.dispatcher
  implicit def rejectionHandler = RejectionHandler.newBuilder()
      .handle {
        case AuthorizationFailedRejection =>
          complete((Forbidden, "Not authorized!"))
      }
      .handle {
        case ValidationRejection(msg, _) =>
          complete((InternalServerError, "Failed validation! " + msg))
      }
      .handleNotFound {
        complete((StatusCodes.NotFound, "This resource not found!"))
      }
      .result()

  implicit def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case _: ArithmeticException =>
        extractUri { uri =>
          println(s"Request to $uri could not be handled normally")
          complete(HttpResponse(InternalServerError, entity = "Internal Server Error occurred"))
        }
    }
  // formats for unmarshalling and marshalling
  implicit val eventFormat = jsonFormat4(MessageEvent)


  var events: List[MessageEvent] = Nil
  events = events :+ MessageEvent(1, "some text", "sender", List("to1", "to2"))

  //TODO: https !!!!

  // (fake) async database query api
  def fetchItem(itemId: Long): Future[Option[MessageEvent]] = Future {
    events.find(o => o.id == itemId)
  }
  def saveEvent(event: MessageEvent): Future[Done] = {
    if (event != null) {
      events = events :+ event
    }
    Future { Done }
  }

  def main(args: Array[String]) {

    val route: Route =
      concat(
        get {
          pathPrefix("event" / LongNumber) { id =>
            // there might be no item for a given id
            val maybeItem: Future[Option[MessageEvent]] = fetchItem(id)

            onSuccess(maybeItem) {
              case Some(item) => complete(item)
              case None       => complete(StatusCodes.NotFound, "No such event!")
            }
          }
        },
        post {
          path("event") {
            entity(as[MessageEvent]) { event =>
              val saved: Future[Done] = saveEvent(event)
              onComplete(saved) { done =>
                complete("Event has been saved")
              }
            }
          }
        }
      )

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)//, connectionContext = https)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
