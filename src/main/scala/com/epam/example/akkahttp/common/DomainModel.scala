package com.epam.example.akkahttp.common

object DomainModel {

  final case class AppEvent(id: Long, name: String, values: List[String])

}
