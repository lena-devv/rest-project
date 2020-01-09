package com.epam.example.akkahttp.client.multiple

import com.typesafe.config.Config

object SinkFactory {

  val CLIENT_TYPE_PARAM = "type"

  def create(conf: Config): Sink = {
    conf.getString(CLIENT_TYPE_PARAM) match {
      case "https" => new HttpsSink()
      case "smtp" => new SmtpSink()
    }
  }
}
