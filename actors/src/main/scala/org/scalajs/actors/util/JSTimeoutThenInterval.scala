package org.scalajs.actors
package util

import scala.scalajs.js

import scala.concurrent.duration.{Duration, FiniteDuration}

class JSTimeoutThenIntervalTask(initialDelay: FiniteDuration,
    interval: FiniteDuration, task: => Any) extends Cancellable {

  private[this] var underlying: Cancellable = JSTimeoutTask(initialDelay) {
    underlying = JSIntervalTask(interval) {
      task
    }
    task
  }

  def isCancelled: Boolean = underlying.isCancelled

  def cancel(): Boolean = underlying.cancel()
}

object JSTimeoutThenIntervalTask {
  def apply(initialDelay: FiniteDuration, interval: FiniteDuration)(
      task: => Any): JSTimeoutThenIntervalTask =
    new JSTimeoutThenIntervalTask(initialDelay, interval, task)
}
