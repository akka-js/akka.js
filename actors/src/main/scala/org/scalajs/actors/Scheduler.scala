/**
 *  Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package org.scalajs.actors

import scala.scalajs.js

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import util._

/**
 * This exception is thrown by Scheduler.schedule* when scheduling is not
 * possible, e.g. after shutting down the Scheduler.
 */
private case class SchedulerException(msg: String) extends ActorsException(msg)

/**
 * An Akka scheduler service. This one needs one special behavior: if
 * Closeable, it MUST execute all outstanding tasks upon .close() in order
 * to properly shutdown all dispatchers.
 *
 * Furthermore, this timer service MUST throw IllegalStateException if it
 * cannot schedule a task. Once scheduled, the task MUST be executed. If
 * executed upon close(), the task may execute before its timeout.
 *
 * Scheduler implementation are loaded reflectively at ActorSystem start-up
 * with the following constructor arguments:
 *  1) the system’s com.typesafe.config.Config (from system.settings.config)
 *  2) a akka.event.LoggingAdapter
 *  3) a java.util.concurrent.ThreadFactory
 */
trait Scheduler {
  /**
   * Schedules a message to be sent repeatedly with an initial delay and
   * frequency. E.g. if you would like a message to be sent immediately and
   * thereafter every 500ms you would set delay=Duration.Zero and
   * interval=Duration(500, TimeUnit.MILLISECONDS)
   *
   * Java & Scala API
   */
  final def schedule(
    initialDelay: FiniteDuration,
    interval: FiniteDuration,
    receiver: ActorRef,
    message: Any)(implicit executor: ExecutionContext,
                  sender: ActorRef = Actor.noSender): Cancellable =
    schedule(initialDelay, interval) {
      receiver ! message
      //if (receiver.isTerminated)
      //  throw new SchedulerException("timer active for terminated actor")
    }

  /**
   * Schedules a function to be run repeatedly with an initial delay and a
   * frequency. E.g. if you would like the function to be run after 2 seconds
   * and thereafter every 100ms you would set delay = Duration(2, TimeUnit.SECONDS)
   * and interval = Duration(100, TimeUnit.MILLISECONDS)
   *
   * Scala API
   */
  def schedule(
      initialDelay: FiniteDuration,
      interval: FiniteDuration)(f: => Unit)(
      implicit executor: ExecutionContext): Cancellable

  /**
   * Schedules a message to be sent once with a delay, i.e. a time period that has
   * to pass before the message is sent.
   *
   * Java & Scala API
   */
  final def scheduleOnce(delay: FiniteDuration, receiver: ActorRef,
      message: Any)(implicit executor: ExecutionContext,
      sender: ActorRef = Actor.noSender): Cancellable = {
    scheduleOnce(delay) {
      receiver ! message
    }
  }

  /**
   * Schedules a function to be run once with a delay, i.e. a time period that has
   * to pass before the function is run.
   *
   * Scala API
   */
  def scheduleOnce(delay: FiniteDuration)(f: => Unit)(
      implicit executor: ExecutionContext): Cancellable

  /**
   * Schedules a Runnable to be run once with a delay, i.e. a time period that
   * has to pass before the runnable is executed.
   *
   * Java & Scala API
   */
  final def scheduleOnce(delay: FiniteDuration, runnable: Runnable)(
      implicit executor: ExecutionContext): Cancellable = {
    scheduleOnce(delay)(runnable.run())
  }

  /**
   * The maximum supported task frequency of this scheduler, i.e. the inverse
   * of the minimum time interval between executions of a recurring task, in Hz.
   */
  def maxFrequency: Double

}

class EventLoopScheduler extends Scheduler {

  def schedule(
      initialDelay: FiniteDuration,
      interval: FiniteDuration)(f: => Unit)(
      implicit executor: ExecutionContext): Cancellable = {
    JSTimeoutThenIntervalTask(initialDelay, interval)(f)
  }

  def scheduleOnce(delay: FiniteDuration)(f: => Unit)(
      implicit executor: ExecutionContext): Cancellable = {
    JSTimeoutTask(delay)(f)
  }

  def maxFrequency: Double = 1.0 / 0.0004 // as per HTML spec

}
