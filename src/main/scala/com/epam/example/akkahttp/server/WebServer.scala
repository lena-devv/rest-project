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

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.server._
import com.epam.example.akkahttp.common.DomainModel.AppEvent
import com.epam.example.akkahttp.common.JsonSupport
import com.typesafe.scalalogging.Logger
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import spray.json.RootJsonFormat

object WebServer extends JsonSupport {

  val log: Logger = Logger(getClass.getName)

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit val eventFormat:RootJsonFormat[AppEvent] = jsonFormat4(AppEvent)

  implicit def rejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
      .handle {
        case AuthorizationFailedRejection =>
          log.error("The user is not authorized!")
          complete((Forbidden, "Not authorized!"))
      }
      .handle {
        case ValidationRejection(msg, _) =>
          log.error("The validation was failed!")
          complete((InternalServerError, "Failed validation! " + msg))
      }
      .handleNotFound {
          complete((StatusCodes.NotFound, "This resource not found!"))
      }
      .result()

  implicit def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case err: ArithmeticException =>
        extractUri { uri =>
          log.error(s"Request to $uri could not be handled normally", err)
          complete(HttpResponse(InternalServerError, entity = "Internal Server Error occurred"))
        }
    }

  var events: List[AppEvent] = Nil
  events = events :+ AppEvent(1, "some text", "sender", List("to1", "to2"))

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
              case Some(event) =>
                log.info(s"Found event with id=$id: $event")
                complete(event)

              case None =>
                log.error(s"No such event with id: $id!")
                complete(StatusCodes.NotFound, "No such event!")
            }
          }
        },
        post {
          path("event") {
            entity(as[AppEvent]) { event => {
              val saved: Future[Done] = saveEvent(event)
              onComplete(saved) { _ =>
                  log.info(s"Saved event: $event")
                  complete(StatusCodes.OK, "Event has been saved")
                }
              }
            }
          }
        }
      )

    val httpsContext: ConnectionContext = {
      val pathToKeyStore = "keys/server/self-signed-keystore.p12"
      val password = "cert-pass".toCharArray
      //      val alias = "cert-alias"

      val ksStream = getClass.getClassLoader.getResourceAsStream(pathToKeyStore)
      val ks = KeyStore.getInstance("PKCS12")
      ks.load(ksStream, password)

      val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
      keyManagerFactory.init(ks, password)

      val context = SSLContext.getInstance("TLS")
      context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)

//      new HttpsConnectionContext(context)
      ConnectionContext.https(context)
    }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080, connectionContext = httpsContext)
    val codeToExit = "RETURN"
    log.info(s"Server online at https://localhost:8080/\nPress $codeToExit to stop...")
    if (StdIn.readLine().eq(codeToExit)) {
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate()) // and shutdown when done
    }
  }
}
