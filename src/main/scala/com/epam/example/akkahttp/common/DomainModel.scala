package com.epam.example.akkahttp.common

object DomainModel {

  final case class AppEvent(id: Long, text: String, from: String, to: List[String])

}
