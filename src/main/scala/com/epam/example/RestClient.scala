package com.epam.example

import scala.util.{ Failure, Success }
import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethod, HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.pattern.pipe
import akka.util.ByteString

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.io.StdIn
import akka.http.scaladsl.model._

object RestClient {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://localhost:8080/event/1", method = HttpMethods.GET))
    responseFuture.map {
      case response @ HttpResponse(StatusCodes.OK, headers, entity, _) => {
        val headersStr: String = headers.map(header => s"${header.name()}: ${header.value()}").toList.mkString("\n")
        println(s"entity: $entity, headers: $headersStr")
      }
      case _ => sys.error("something wrong")
    }
    responseFuture.onComplete(done => {system.terminate()})
    /*responseFuture.onComplete {
      case Success(res) => println(res)
      case Failure(_)   => sys.error("something wrong")
    }*/
  }
}

/*object RestClient {

  def call(url: String): Unit = {

  }

}*/
