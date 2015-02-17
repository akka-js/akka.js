package akka.scalajs.jsapi

import scala.concurrent.duration.FiniteDuration

import scala.scalajs.js
import js.annotation.JSName

class TimersBase extends js.Object

object Timers extends TimersBase with js.GlobalScope {
  type TimeoutID = Number
  type IntervalID = Number

  @JSName("setTimeout")
  private[jsapi] def setTimeout_impl(func: js.Function0[_],
      delay: Number): TimeoutID = js.native

  @JSName("setInterval")
  private[jsapi] def setInterval_impl(func: js.Function0[_],
      interval: Number): IntervalID = js.native

  def clearTimeout(timeoutID: TimeoutID): Unit = js.native
  def clearInterval(intervalID: IntervalID): Unit = js.native
}

object TimersBase {
  import Timers.{TimeoutID, IntervalID}

  implicit class Ops(val self: Timers.type) extends AnyVal {
    def setTimeout(func: js.Function0[_], delay: Number): TimeoutID =
      Timers.setTimeout_impl(func, delay)
    def setTimeout(delay: Number)(body: => Any): TimeoutID =
      Timers.setTimeout_impl(() => body, delay)
    def setTimeout(delay: FiniteDuration)(body: => Any): TimeoutID =
      Timers.setTimeout_impl(() => body, delay.toMillis)

    def setInterval(func: js.Function0[_], interval: Number): IntervalID =
      Timers.setInterval_impl(func, interval)
    def setInterval(interval: Number)(body: => Any): IntervalID =
      Timers.setInterval_impl(() => body, interval)
    def setInterval(interval: FiniteDuration)(body: => Any): IntervalID =
      Timers.setInterval_impl(() => body, interval.toMillis)

    def setImmediate(body: => Any): Unit =
      setTimeout(0)(body)
  }
}
