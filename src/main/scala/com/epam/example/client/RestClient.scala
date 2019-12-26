package com.epam.example.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.{Get, Post}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.epam.example.client.DomainModel.MessageEvent
import spray.json.DefaultJsonProtocol.jsonFormat4
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

import spray.json.DefaultJsonProtocol._

object RestClient extends JsonSupport {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    implicit val eventJsonFormat: RootJsonFormat[MessageEvent] = jsonFormat4(MessageEvent)

//    callGet()(executionContext, system, materializer)
    callPost()(executionContext, system, materializer)
  }

  def callGet()(implicit executionContext: ExecutionContext, actorSystem: ActorSystem, materializer: ActorMaterializer) = {

    val getMessageRespFuture: Future[HttpResponse] = Http().singleRequest(Get("http://localhost:8080/event/2"))
    getMessageRespFuture.map {
      case response @ HttpResponse(StatusCodes.OK, headers, entity, _) => {
        val event: Future[MessageEvent] = json4sUnmarshaller[MessageEvent].apply(response.entity)
        event.onComplete{
          case Success(res) => {
            println(res)
          }
          case Failure(err) => {
            println(err)
          }
        }
      }
      case someErr => {
        println(s"Something wrong: $someErr")
      }
    }
    getMessageRespFuture.onComplete(done => {
      actorSystem.terminate()
    })
  }

  def callPost()(implicit executionContext: ExecutionContext, actorSystem: ActorSystem, materializer: ActorMaterializer) = {
    val sendMessageRespFuture: Future[HttpResponse] = Http().singleRequest(
      Post("http://localhost:8080/event", MessageEvent(3, "message text", "Mike", List("Jane", "John")))
    )
    sendMessageRespFuture.map {
      case response @ HttpResponse(StatusCodes.OK, headers, entity, _) => {
        val body: Future[String] = Unmarshal(entity).to[String]
        body.onComplete {
          case Success(res) => {
            println(s"Status 200, Body: $res")
          }
          case Failure(smth) => {
            println("Error occurred")
          }
        }
      }
      case _ => sys.error("Something wrong")
    }

    sendMessageRespFuture.onComplete(done => {actorSystem.terminate()})
  }
}
