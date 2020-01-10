package com.epam.example.akkahttp.client.multiple

import com.epam.example.akkahttp.common.DomainModel.AppEvent
import com.typesafe.config.Config


trait Sink {

  def init(conf: Config)

  def write(conf: Config, event: AppEvent)

  def close(conf: Config)
}
