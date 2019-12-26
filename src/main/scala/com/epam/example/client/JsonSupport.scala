package com.epam.example.client

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.{DefaultFormats, Formats, native}

trait JsonSupport extends Json4sSupport {
    implicit val serialization = native.Serialization
    implicit def json4sFormats: Formats = DefaultFormats
}
