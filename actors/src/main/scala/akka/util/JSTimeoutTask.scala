package akka.util

import scala.concurrent.duration.FiniteDuration

import akka.scalajs.jsapi.Timers
import akka.actor.Cancellable

class JSTimeoutTask(delay: FiniteDuration, task: => Any) extends Cancellable {
  private[this] var underlying: Timers.TimeoutID =
    Timers.setTimeout(delay)(task)

  def isCancelled: Boolean = underlying ne null

  def cancel(): Boolean = {
    if (isCancelled) false
    else {
      Timers.clearTimeout(underlying)
      underlying = null
      true
    }
  }
}

object JSTimeoutTask {
  def apply(duration: FiniteDuration)(task: => Any): JSTimeoutTask =
    new JSTimeoutTask(duration, task)
}
