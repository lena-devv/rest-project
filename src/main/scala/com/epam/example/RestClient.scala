package com.epam.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.stream.ActorMaterializer

import scala.io.StdIn

object RestClient {

  def main(args: Array[String]): Unit = {
    val client: RestClient = new RestClient()
    client.call("")
  }
}

class RestClient {

  def call(url: String): Unit = {

  }

}
