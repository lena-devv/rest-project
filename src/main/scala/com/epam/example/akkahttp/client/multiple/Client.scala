package com.epam.example.akkahttp.client.multiple

import java.util.Collections

import com.epam.example.akkahttp.common.DomainModel.AppEvent
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger


object Client {

  val log: Logger = Logger(getClass.getName)

  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory.load() //.load("application.conf") (by default)
      .getConfig("com.epam.example")

    val appName = conf.getString("app-name")
    log.info(s"'$appName' started")

    val clientConf = conf.getConfig("client")
    val client: Sink = SinkFactory.create(clientConf)
    client.init(clientConf)

    for (i <- 1 to 1) {
      log.info("Try send " + i + "th message...")
      val event = AppEvent(i, "Event #" + i, "Author", List("user-to#1", "user-to#2"))
      client.write(ConfigFactory.parseMap(Collections.singletonMap("param", i)), event)
    }

    try {
      Thread.sleep(5000)
    } catch {
      case e: Exception => {
        log.error("Error occurred: " + e)
      }
    } finally {
      client.close(ConfigFactory.empty())
    }
  }
}
