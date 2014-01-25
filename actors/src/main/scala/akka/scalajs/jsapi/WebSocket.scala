package akka.scalajs.jsapi

import scala.scalajs.js

class WebSocket(url: js.String) extends js.Object with EventTarget {
  def send(message: js.String): Unit = ???

  def close(code: js.Number, reason: js.String): Unit = ???
  def close(code: js.Number): Unit = ???
  def close(): Unit = ???
}
