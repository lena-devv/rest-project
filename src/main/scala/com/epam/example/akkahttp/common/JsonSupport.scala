package com.epam.example.akkahttp.common

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.{DefaultFormats, Formats, Serialization, native}

trait JsonSupport extends Json4sSupport {
    implicit val serialization: Serialization = native.Serialization
    implicit def json4sFormats: Formats = DefaultFormats
}
