package org.scalajs.actors
package util

import scala.scalajs.js

import scala.concurrent.duration.{Duration, FiniteDuration}

class JSIntervalTask(interval: FiniteDuration, task: => Any) extends Cancellable {
  private[this] var underlying: js.Dynamic =
    js.Dynamic.global.setInterval({ () =>
      task
    }, interval.toMillis)

  def isCancelled: Boolean = underlying ne null

  def cancel(): Boolean = {
    if (isCancelled) false
    else {
      js.Dynamic.global.clearInterval(underlying)
      underlying = null
      true
    }
  }
}

object JSIntervalTask {
  def apply(interval: FiniteDuration)(task: => Any): JSIntervalTask =
    new JSIntervalTask(interval, task)
}
