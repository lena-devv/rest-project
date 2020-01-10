package com.epam.example.akkahttp.client.multiple

import com.epam.example.akkahttp.common.DomainModel.AppEvent
import com.typesafe.config.Config


trait Sink {

  def init(httpConf: Config)

  def write(httpConf: Config, event: AppEvent)

  def close(httpConf: Config)
}
