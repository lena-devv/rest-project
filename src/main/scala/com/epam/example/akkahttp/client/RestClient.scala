package com.epam.example.akkahttp.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.{Get, Post}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.stream.ActorMaterializer
import com.epam.example.akkahttp.common.DomainModel.MessageEvent
import com.epam.example.akkahttp.common.JsonSupport
import spray.json.DefaultJsonProtocol.jsonFormat4
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import spray.json.DefaultJsonProtocol._

object RestClient extends JsonSupport {

  def main(args: Array[String]): Unit = {
    var protocol = "http"
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
    implicit val eventJsonFormat: RootJsonFormat[MessageEvent] = jsonFormat4(MessageEvent)

    callGet()(executionContext, system, materializer, baseUrl)
//    callPost()(executionContext, system, materializer, baseUrl)
  }

  def callGet()(implicit executionContext: ExecutionContext, actorSystem: ActorSystem, materializer: ActorMaterializer,
                url: String) = {
    val id: Long = 3
    val getMessageRespFuture: Future[HttpResponse] = Http().singleRequest(Get(url + "/event/" + id))
    getMessageRespFuture.map {
      case response @ HttpResponse(StatusCodes.OK, headers, entity, _) => {
        val event: Future[MessageEvent] = json4sUnmarshaller[MessageEvent].apply(response.entity)
        event.onComplete{
          case Success(res) => println("Received message: " + res)
          case Failure(err) => println(err)
        }
      }
      case respError => println(s"Something wrong: $respError")
    }
    getMessageRespFuture.onComplete(_ => { actorSystem.terminate() })
  }

  def callPost()(implicit executionContext: ExecutionContext, actorSystem: ActorSystem, materializer: ActorMaterializer,
                 url: String) = {
    val event = MessageEvent(3, "Message text", "Mike", List("Jane", "John"))
    val sendMessageRespFuture: Future[HttpResponse] = Http().singleRequest(Post(url + "/event", event))
    sendMessageRespFuture.map {
      case response @ HttpResponse(StatusCodes.OK, headers, entity, _) => println(s"Sent event successfully")
      case _ => sys.error("Something wrong")
    }
    sendMessageRespFuture.onComplete(_ => { actorSystem.terminate() })
  }
}
