package akka.util

import scala.concurrent.duration.FiniteDuration

import scala.scalajs.js.timers.{SetIntervalHandle, setInterval, clearInterval}
import akka.actor.Cancellable

class JSIntervalTask(interval: FiniteDuration, task: => Any) extends Cancellable {
  private[this] var underlying: Option[SetIntervalHandle] =
    Some(setInterval(interval)(task))

  def isCancelled: Boolean = underlying.isEmpty

  def cancel(): Boolean = {
    if (isCancelled) false
    else {
      clearInterval(underlying.get)
      underlying = None
      true
    }
  }
}

object JSIntervalTask {
  def apply(interval: FiniteDuration)(task: => Any): JSIntervalTask =
    new JSIntervalTask(interval, task)
}
