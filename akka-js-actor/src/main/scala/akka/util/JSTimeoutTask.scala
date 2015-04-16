package akka.util

import scala.concurrent.duration.FiniteDuration

import scala.scalajs.js.timers.{SetTimeoutHandle, setTimeout, clearTimeout}
import akka.actor.Cancellable

class JSTimeoutTask(delay: FiniteDuration, task: => Any) extends Cancellable {
  private[this] var underlying: Option[SetTimeoutHandle] =
    Some(setTimeout(delay)(task))

  def isCancelled: Boolean = underlying.isEmpty

  def cancel(): Boolean = {
    if (isCancelled) false
    else {
      clearTimeout(underlying.get)
      underlying = None
      true
    }
  }
}

object JSTimeoutTask {
  def apply(duration: FiniteDuration)(task: => Any): JSTimeoutTask =
    new JSTimeoutTask(duration, task)
}
