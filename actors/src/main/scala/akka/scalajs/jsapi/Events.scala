package akka.scalajs.jsapi

import scala.scalajs.js

trait EventTarget extends js.Object {
  def addEventListener(`type`: js.String,
      listener: js.Function1[Event, _],
      useCapture: js.Boolean): Unit = ???
  def removeEventListener(`type`: js.String,
      listener: js.Function1[Event, _],
      useCapture: js.Boolean): Unit = ???
}

class Event extends js.Object {

}

trait MessageEvent extends Event {
  val data: js.Dynamic = ???
}

trait CloseEvent extends Event {
  val wasClean: js.Boolean = ???
  val code: js.Number = ???
  val reason: js.String = ???
}
