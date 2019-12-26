package com.epam.example

import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.http.scaladsl.unmarshalling.Unmarshal

object RestClient {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://localhost:8080/event/1", method = HttpMethods.GET))
    responseFuture.map {
      case response @ HttpResponse(StatusCodes.OK, headers, entity, _) => {
        val headersStr: String = headers.map(header => s"${header.name()}: ${header.value()}").toList.mkString("\n")
        val body: Future[String] = Unmarshal(entity).to[String]
        body.onComplete {
          case Success(res) => println(s"Status 200, Body: $res, headers: $headersStr")
          case Failure(_) => println("Error occurred")
        }
      }
      case _ => sys.error("Something wrong")
    }

    responseFuture.onComplete(done => {system.terminate()})
  }
}
