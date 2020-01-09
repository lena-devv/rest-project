package com.epam.example.akkahttp.client.multiple

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import java.security.cert.{Certificate, CertificateFactory}

import akka.actor.ActorSystem
import akka.http.scaladsl.client.RequestBuilding.{Post, Put}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.{Http, HttpExt, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import com.epam.example.akkahttp.common.DomainModel.AppEvent
import com.epam.example.akkahttp.common.JsonSupport
import com.typesafe.config.{Config, ConfigException}
import com.typesafe.scalalogging.Logger
import javax.net.ssl.{SSLContext, SSLParameters, TrustManagerFactory}
import spray.json.DefaultJsonProtocol.jsonFormat4
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContextExecutor, Future}
import spray.json.DefaultJsonProtocol._

import scala.util.{Failure, Success}


class HttpsSink extends Sink with JsonSupport {

  val log: Logger = Logger(getClass.getName)

  var clientHttpsContext: HttpsConnectionContext = _
  var httpExt: HttpExt = _

  var url: String = _
  var method: String = _

  implicit var system: ActorSystem = _
  implicit var materializer: ActorMaterializer = _
  implicit var executionContext: ExecutionContextExecutor = _

  override def connect(httpConf: Config): Unit = {
    system = ActorSystem()
    materializer = ActorMaterializer()
    executionContext = system.dispatcher
    implicit val eventJsonFormat: RootJsonFormat[AppEvent] = jsonFormat4(AppEvent)
    httpExt = Http()

    log.info("Created Https Client")
    clientHttpsContext = initClientHttpsContext(httpConf)
    url = httpConf.getString("url")
    method = httpConf.getString("method")
  }

  override def write(httpConf: Config, event: AppEvent): Unit = {
    // TODO: create connection once instead of single request
    val req: HttpRequest = if (method.equals("POST")) Post(url, event) else Put(url, event)
    val sendMessageRespFuture: Future[HttpResponse] = httpExt.singleRequest(req, connectionContext = clientHttpsContext)
    sendMessageRespFuture.onComplete {
      case Success(response: HttpResponse) => {
        response match {
          case response@HttpResponse(StatusCodes.OK, headers, entity, _) =>
            log.info(s"Sent event successfully: " + response)

          case err: HttpResponse => {
            log.error("Something wrong: " + err)
          }
        }
      }
      case Failure(err: Exception) => {
        log.error("Error: " + err)
      }
    }
  }

  override def close(httpConf: Config): Unit = {
    log.info("Terminate Https Client...")
    system.terminate()
  }


  private def initClientHttpsContext(httpConf: Config): HttpsConnectionContext = {
    try {
      val certificateResourcePath = httpConf.getString("tls_cert")
      log.info("Protocol: HTTPS")

      val certStore = KeyStore.getInstance(KeyStore.getDefaultType)
      certStore.load(null, null)

      val certStream: InputStream = getClass.getClassLoader.getResourceAsStream(certificateResourcePath)
      require(certStream ne null, s"SSL certificate not found!")

      val cert: Certificate = CertificateFactory.getInstance("X.509").generateCertificate(certStream)
      val certificateAlias = "cert-alias"
      certStore.setCertificateEntry(certificateAlias, cert)

      val certManagerFactory = TrustManagerFactory.getInstance("SunX509")
      certManagerFactory.init(certStore)

      val context = SSLContext.getInstance("TLS")
      context.init(null, certManagerFactory.getTrustManagers, new SecureRandom)

      val params = new SSLParameters()
      params.setEndpointIdentificationAlgorithm("https")

      new HttpsConnectionContext(context, sslParameters = Some(params))
    } catch {
      case e: ConfigException.Missing => {
        log.info("Protocol: HTTP")
        httpExt.createDefaultClientHttpsContext()
      }
    }
  }
}
