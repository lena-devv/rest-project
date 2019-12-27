package com.epam.example.client

object DomainModel {

  final case class MessageEvent(id: Long, text: String, from: String, to: List[String])

}
