package com.epam.example.akkahttp.client.multiple

import com.epam.example.akkahttp.common.DomainModel.AppEvent
import com.typesafe.config.Config
import com.typesafe.scalalogging.Logger


class SmtpSink extends Sink {

  val log: Logger = Logger(getClass.getName)

  override def connect(httpConf: Config): Unit = ???

  override def write(httpConf: Config, event: AppEvent): Unit = ???

  override def close(httpConf: Config): Unit = {
    log.info("Terminate SMTP Client...")
  }
}
