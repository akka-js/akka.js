package akka.actor

import scala.scalajs.js.timers._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.ThreadFactory
import com.typesafe.config.Config
import akka.event.LoggingAdapter
import scala.scalajs.js.annotation

@annotation.JSExport
class EventLoopScheduler(config: Config,
                         log: LoggingAdapter,
                         threadFactory: ThreadFactory) extends Scheduler {

  def schedule(
      initialDelay: FiniteDuration,
      interval: FiniteDuration,
      runnable: Runnable)
      (implicit executor: ExecutionContext): Cancellable =
        JSTimeoutThenIntervalTask(initialDelay, interval)(runnable.run())

  def scheduleOnce(
      delay: FiniteDuration,
      runnable: Runnable)
      (implicit executor: ExecutionContext): Cancellable =
        JSTimeoutTask(delay)(runnable.run())

  def maxFrequency: Double = 1.0 / 0.0004 // as per HTML spec

  private case class JSTimeoutTask(delay: FiniteDuration)(task: => Any) extends Cancellable {
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

  private case class JSIntervalTask(interval: FiniteDuration)(task: => Any) extends Cancellable {
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

  private case class JSTimeoutThenIntervalTask(initialDelay: FiniteDuration,
      interval: FiniteDuration)(task: => Any) extends Cancellable {

    private[this] var underlying: Cancellable = JSTimeoutTask(initialDelay) {
      underlying = JSIntervalTask(interval) {
        task
      }
      task
    }

    def isCancelled: Boolean = underlying.isCancelled

    def cancel(): Boolean = underlying.cancel()
  }
}

class LightArrayRevolverScheduler(
    config: Config,
    log: LoggingAdapter,
    threadFactory: ThreadFactory) extends
    EventLoopScheduler(config, log, threadFactory)
