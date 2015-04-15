package akka.scalajs.jsapi

import scala.scalajs.js

class WebSocket(url: String) extends js.Object with EventTarget {
  def send(message: String): Unit = js.native

  def close(code: Number, reason: String): Unit = js.native
  def close(code: Number): Unit = js.native
  def close(): Unit = js.native
}
