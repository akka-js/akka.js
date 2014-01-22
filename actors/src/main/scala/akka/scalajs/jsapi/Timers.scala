package akka.scalajs.jsapi

import scala.concurrent.duration.FiniteDuration

import scala.scalajs.js
import js.annotation.JSName

class TimersBase extends js.Object

object Timers extends TimersBase with js.GlobalScope {
  type TimeoutID = js.Number
  type IntervalID = js.Number

  @JSName("setTimeout")
  private[jsapi] def setTimeout_impl(func: js.Function0[_],
      delay: js.Number): TimeoutID = ???

  @JSName("setInterval")
  private[jsapi] def setInterval_impl(func: js.Function0[_],
      interval: js.Number): IntervalID = ???

  def clearTimeout(timeoutID: TimeoutID): Unit = ???
  def clearInterval(intervalID: IntervalID): Unit = ???
}

object TimersBase {
  import Timers.{TimeoutID, IntervalID}

  implicit class Ops(val self: Timers.type) extends AnyVal {
    def setTimeout(func: js.Function0[_], delay: js.Number): TimeoutID =
      Timers.setTimeout_impl(func, delay)
    def setTimeout(delay: js.Number)(body: => Any): TimeoutID =
      Timers.setTimeout_impl(() => body, delay)
    def setTimeout(delay: FiniteDuration)(body: => Any): TimeoutID =
      Timers.setTimeout_impl(() => body, delay.toMillis)

    def setInterval(func: js.Function0[_], interval: js.Number): IntervalID =
      Timers.setInterval_impl(func, interval)
    def setInterval(interval: js.Number)(body: => Any): IntervalID =
      Timers.setInterval_impl(() => body, interval)
    def setInterval(interval: FiniteDuration)(body: => Any): IntervalID =
      Timers.setInterval_impl(() => body, interval.toMillis)

    def setImmediate(body: => Any): Unit =
      setTimeout(0)(body)
  }
}
