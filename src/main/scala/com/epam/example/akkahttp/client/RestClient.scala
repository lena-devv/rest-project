package com.epam.example.akkahttp.client

import java.io.InputStream
import java.security.cert.{Certificate, CertificateFactory}
import java.security.{KeyStore, SecureRandom}

import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt, HttpsConnectionContext}
import akka.http.scaladsl.client.RequestBuilding.{Get, Post}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.epam.example.akkahttp.common.DomainModel.AppEvent
import com.epam.example.akkahttp.common.JsonSupport
import com.typesafe.scalalogging.Logger
import javax.net.ssl.{SSLContext, SSLParameters, TrustManagerFactory}
import spray.json.DefaultJsonProtocol.jsonFormat4
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import spray.json.DefaultJsonProtocol._


object RestClient extends JsonSupport {

  val log: Logger = Logger(getClass.getName)

  def main(args: Array[String]): Unit = {
    var protocol = "https"
    var host = "localhost"
    var port = "8080"

    if (args.length == 3) {
      protocol = args(0)
      host = args(1)
      port = args(2)
    }

    val baseUrl = s"$protocol://$host:$port"

    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    implicit val eventJsonFormat: RootJsonFormat[AppEvent] = jsonFormat4(AppEvent)
    val httpExt: HttpExt = Http()

    val clientHttpsContext: HttpsConnectionContext =
      initClientHttpsContext("keys/client/self-signed-localhost.crt")

    val responseFuture: Future[_] = callGet(baseUrl, clientHttpsContext, httpExt)(executionContext, system, materializer)
//    val responseFuture: Future[_] = callPost(baseUrl, clientHttpsContext, httpExt)(executionContext, system, materializer)

    responseFuture.onComplete(resp => {
      log.info(resp.toString)
      system.terminate()
    })
  }

  def initClientHttpsContext(certificateResourcePath: String): HttpsConnectionContext = {
    val certStore = KeyStore.getInstance(KeyStore.getDefaultType)
    certStore.load(null, null)

    val certStream: InputStream = getClass.getClassLoader.getResourceAsStream(certificateResourcePath)
    require(certStream ne null, s"SSL certificate not found!")

    val cert: Certificate = CertificateFactory.getInstance("X.509").generateCertificate(certStream)
    certStore.setCertificateEntry("ca", cert)

    val certManagerFactory = TrustManagerFactory.getInstance("SunX509")
    certManagerFactory.init(certStore)

    val context = SSLContext.getInstance("TLS")
    context.init(null, certManagerFactory.getTrustManagers, new SecureRandom)

    val params = new SSLParameters()
    params.setEndpointIdentificationAlgorithm("https")

    new HttpsConnectionContext(context, sslParameters = Some(params))
  }

  def callGet(url: String, clientHttpsContext: HttpsConnectionContext, httpExt: HttpExt)
             (implicit executionContext: ExecutionContext, actorSystem: ActorSystem,
              materializer: ActorMaterializer): Future[_] = {

    val id: Long = 1 //3
    val getMessageRespFuture: Future[HttpResponse] = httpExt.singleRequest(Get(url + "/event/" + id),
      connectionContext = clientHttpsContext)
    getMessageRespFuture.map {
      case response @ HttpResponse(StatusCodes.OK, headers, entity, _) => {
        val event: Future[AppEvent] = Unmarshal(response).to[AppEvent]
        event.onComplete{
          case Success(res) => {
            log.info("Received message: " + res)
          }
          case Failure(err) => {
            log.error("Error occurred: ", err)
          }
        }
      }
      case respError => {
        log.info(s"Something wrong: $respError")
      }
    }
    getMessageRespFuture
  }

  def callPost(url: String, clientHttpsContext: HttpsConnectionContext, httpExt: HttpExt)
              (implicit executionContext: ExecutionContext, actorSystem: ActorSystem,
               materializer: ActorMaterializer): Future[_] = {

    val event = AppEvent(3, "Message text", "Mike", List("Jane", "John"))
    val sendMessageRespFuture: Future[HttpResponse] = httpExt.singleRequest(Post(url + "/event", event),
      connectionContext = clientHttpsContext)
    sendMessageRespFuture.map {
      case response @ HttpResponse(StatusCodes.OK, headers, entity, _) => log.info(s"Sent event successfully")
      case _ => log.error("Something wrong")
    }
    sendMessageRespFuture
  }
}
