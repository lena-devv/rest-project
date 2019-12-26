package com.epam.example.server

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, HttpResponse}
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import spray.json.{JsObject, JsString, JsValue}

trait RestHttpSupport extends Directives {

  @inline def error  (msg: String): String = JsObject("error"   -> JsString(msg)).prettyPrint
  @inline def success(msg: String): String = JsObject("success" -> JsString(msg)).prettyPrint

  @inline def error  (msg: JsValue): String = JsObject("error"   -> msg).prettyPrint
  @inline def success(msg: JsValue): String = JsObject("success" -> msg).prettyPrint

  @inline def wrap(block: => JsValue): StandardRoute =
    complete(
      try {
        HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, success(block)))
      } catch {
        case e: Exception =>
          HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, error(e.getMessage)))
      }
    )

  @inline def completeAsJson[T](requestHeaders: Seq[HttpHeader])
                               (body: T => StandardRoute)
                               (implicit um: FromRequestUnmarshaller[T]): Route = {
    import akka.http.scaladsl.model.MediaTypes.`application/json`
    if (new MediaTypeNegotiator(requestHeaders).isAccepted(`application/json`)) {
      entity(as[T]) { body }
    } else {
      reject(UnsupportedRequestContentTypeRejection(Set(`application/json`)))
    }
  }

  @inline def postAsJson[T](body: T => StandardRoute)
                           (implicit um: FromRequestUnmarshaller[T]): Route = {
    (post & extract(_.request.headers)) { requestHeaders =>
      completeAsJson[T](requestHeaders) { body }
    }
  }
}
