package com.epam.example.akkahttp.server

import java.security.{KeyStore, SecureRandom}

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http}
import akka.http.scaladsl.model.StatusCodes.{Forbidden, InternalServerError}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import spray.json.DefaultJsonProtocol.jsonFormat4

import scala.concurrent.Future
import scala.io.StdIn
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.server._
import com.epam.example.akkahttp.common.DomainModel.AppEvent
import com.epam.example.akkahttp.common.JsonSupport
import javax.net.ssl.{KeyManagerFactory, SSLContext}

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
  implicit val eventFormat = jsonFormat4(AppEvent)


  var events: List[AppEvent] = Nil
  events = events :+ AppEvent(1, "some text", "sender", List("to1", "to2"))

  //TODO: https !!!!

  // (fake) async database query api
  def fetchItem(itemId: Long): Future[Option[AppEvent]] = Future {
    events.find(o => o.id == itemId)
  }
  def saveEvent(event: AppEvent): Future[Done] = {
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
            val optionalEvent: Future[Option[AppEvent]] = fetchItem(id)
            onSuccess(optionalEvent) {
              case Some(event) => complete(event)
              case None        => complete(StatusCodes.NotFound, "No such event!")
            }
          }
        },
        post {
          path("event") {
            entity(as[AppEvent]) { event => {
                  val saved: Future[Done] = saveEvent(event)
                  onComplete(saved) { _ => complete(StatusCodes.OK, "Event has been saved")
                }
              }
            }
          }
        }
      )

    val httpsContext: ConnectionContext = {

      val ksStream = getClass.getClassLoader.getResourceAsStream("keys/server/self-signed-keystore.p12")
      val ks = KeyStore.getInstance("PKCS12")
      val password = "cert-pass".toCharArray
      ks.load(ksStream, password)

      val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      keyManagerFactory.init(ks, password)

//      val alias = "cert-alias"
      val context = SSLContext.getInstance("TLS")
      context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)

//      new HttpsConnectionContext(context)

      ConnectionContext.https(context)
    }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080, connectionContext = httpsContext)
    println(s"Server online at https://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
