package akka.util

import scala.concurrent.duration.FiniteDuration

import akka.scalajs.jsapi.Timers
import akka.actor.Cancellable

class JSIntervalTask(interval: FiniteDuration, task: => Any) extends Cancellable {
  private[this] var underlying: Timers.IntervalID =
    Timers.setInterval(interval)(task)

  def isCancelled: Boolean = underlying ne null

  def cancel(): Boolean = {
    if (isCancelled) false
    else {
      Timers.clearInterval(underlying)
      underlying = null
      true
    }
  }
}

object JSIntervalTask {
  def apply(interval: FiniteDuration)(task: => Any): JSIntervalTask =
    new JSIntervalTask(interval, task)
}
