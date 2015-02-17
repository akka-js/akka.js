package akka.scalajs.jsapi

import scala.scalajs.js

trait EventTarget extends js.Object {
  def addEventListener(`type`: String,
      listener: js.Function1[Event, _],
      useCapture: Boolean): Unit = js.native
  def removeEventListener(`type`: String,
      listener: js.Function1[Event, _],
      useCapture: Boolean): Unit = js.native
}

class Event extends js.Object {

}

trait MessageEvent extends Event {
  val data: js.Dynamic = js.native
}

trait CloseEvent extends Event {
  val wasClean: Boolean = js.native
  val code: Number = js.native
  val reason: String = js.native
}
