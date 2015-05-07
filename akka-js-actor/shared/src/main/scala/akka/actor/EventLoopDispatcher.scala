package akka.actor

import akka.util.{JSTimeoutTask, JSTimeoutThenIntervalTask}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class EventLoopScheduler extends Scheduler {

  def schedule(
      initialDelay: FiniteDuration, 
      interval: FiniteDuration,
      runnable: Runnable)
      (implicit executor: ExecutionContext): Cancellable = {
    schedule(initialDelay, interval) {
      runnable.run()
    }
  }
  
  def scheduleOnce(
      delay: FiniteDuration,
      runnable: Runnable)
      (implicit executor: ExecutionContext): Cancellable = {
    scheduleOnce(delay) {
      runnable.run()
    }
  }
  

  override def schedule(
                         initialDelay: FiniteDuration,
                         interval: FiniteDuration)(f: => Unit)(
                         implicit executor: ExecutionContext): Cancellable = {
    JSTimeoutThenIntervalTask(initialDelay, interval)(f)
  }

  override def scheduleOnce(delay: FiniteDuration)(f: => Unit)(
    implicit executor: ExecutionContext): Cancellable = {
    JSTimeoutTask(delay)(f)
  }

  def maxFrequency: Double = 1.0 / 0.0004 // as per HTML spec

}