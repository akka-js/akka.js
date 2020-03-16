package akka.testkit

import java.util.concurrent.TimeUnit

import scala.util.{Try, Success, Failure }
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.concurrent.duration.Duration

import scala.scalajs.js
import scala.scalajs.js.Dynamic.global
import scala.scalajs.js.timers._
import scala.scalajs.runtime.UndefinedBehaviorError
import scala.collection.mutable.{ Queue, ArrayBuffer }

object ManagedEventLoop {

  private val jsSetTimeout = global.setTimeout
  private val jsSetImmediate = global.setImmediate
  private val jsSetInterval = global.setInterval
  private val jsClearTimeout = global.clearTimeout
  private val jsClearInterval = global.clearInterval

  def timer() = Duration(System.currentTimeMillis, TimeUnit.MILLISECONDS)

  type JSFun = js.Function0[Any]
  private abstract class Event(handler: JSFun) {
    val creationDate = timer()
    var externalQueueH: Option[js.Dynamic] = None
    def clear(): Unit
    def globalize(): Unit
    def hasToRun(now: Duration): Boolean
    val hasToBeRemoved: Boolean

    def run(): Unit = handler()
  }
  private final class TimeoutEvent(handler: => JSFun, time: Double) extends Event(handler) {
    def clear(): Unit = externalQueueH.map(x => jsClearTimeout(x))
    def globalize(): Unit =
      externalQueueH = Some(
        jsSetTimeout(() => {
          handler()
          events -= this
        }, time.asInstanceOf[js.Any])
      )
    def hasToRun(now: Duration): Boolean = {
      (now > creationDate + Duration(time, TimeUnit.MILLISECONDS))
    }
    val hasToBeRemoved: Boolean = true
  }
  private final object TimeoutEvent {
    def apply(handler: => JSFun, time: Double) = new TimeoutEvent(handler, time)
  }
  private final class IntervalEvent(handler: => JSFun, time: Double) extends Event(handler) {
    var lastRan = creationDate
    def clear(): Unit = externalQueueH.map(x => jsClearInterval(x))
    def globalize(): Unit =
      externalQueueH = Some(
        jsSetInterval(() => {
          handler()
          lastRan = timer
        }, time.asInstanceOf[js.Any])
      )
    def hasToRun(now: Duration): Boolean = {
      (now > lastRan + Duration(time, TimeUnit.MILLISECONDS))
    }
    val hasToBeRemoved: Boolean = false
  }
  private final object IntervalEvent {
    def apply(handler: => JSFun, time: Double) = new IntervalEvent(handler, time)
  }

  private val events = new ArrayBuffer[Event]()

  private var isBlocking: Int = 0

  def setBlocking = {
    if (isBlocking == 0)
      events.foreach(_.clear())

    isBlocking += 1
  }

  def resetBlocking = {
    isBlocking -= 1
    if (isBlocking == 0)
      events.foreach(_.globalize())
  }

  def tick(): Duration = {
    val now = timer()

    val toBeProcessed = events.filter(_.hasToRun(now))

    val toBeRemoved = toBeProcessed.filter(_.hasToBeRemoved)
    events --= toBeRemoved

    toBeProcessed.foreach(_.run())

    events --= toBeRemoved

    now
  }

  def reset: Unit = {
    global.setTimeout = jsSetTimeout
    global.setImmediate = jsSetImmediate
    global.setInterval = jsSetInterval
    global.clearTimeout = jsClearTimeout
    global.clearInterval = jsClearInterval
  }

  def manage: Unit = {
    global.setTimeout = { (f: js.Function0[_], delay: Number) =>
      if(f.toString() != "undefined") {
        val event = TimeoutEvent(f, delay.doubleValue())
        if (isBlocking == 0)
          event.globalize
        events += event
        event.asInstanceOf[SetTimeoutHandle]
      }
    }
    global.setImmediate = { (f: js.Function0[_]) =>
      if(f.toString() != "undefined") {
        val event = TimeoutEvent(f, 0)
        if (isBlocking == 0)
          event.globalize
        events += event
        event.asInstanceOf[SetTimeoutHandle]
      }
    }
    global.setInterval = { (f: js.Function0[_], interval: Number) =>
      val event = IntervalEvent(f, interval.doubleValue())
      if (isBlocking == 0)
        event.globalize()
      events += event
      event.asInstanceOf[SetIntervalHandle]
    }
    global.clearTimeout = (event: SetTimeoutHandle) => {
      event.asInstanceOf[TimeoutEvent].clear()
      events -= event.asInstanceOf[TimeoutEvent]
    }
    global.clearInterval = (event: SetIntervalHandle) => {
      event.asInstanceOf[IntervalEvent].clear()
      events -= event.asInstanceOf[IntervalEvent]
    }
  }
}

sealed trait CanAwait

object AwaitPermission extends CanAwait


trait Awaitable[T] {
   def ready(atMost: Duration)(implicit permit: CanAwait): Awaitable.this.type
   def result(atMost: Duration)(implicit permit: CanAwait): T
}

case class LastRan(time: Double)

object Await {
  def ready[T](f: Future[T], atMost: Duration): f.type = {
    result[T](f, atMost)
    f
  }

  def ready[T](awaitable: Awaitable[T], atMost: Duration): awaitable.type =
    awaitable.ready(atMost)(AwaitPermission)

  def result[T](f: Future[T]): T =
    result[T](f, Duration.Inf)

  def result[T](f: Future[T], atMost: Duration): T = {
    val initTime = ManagedEventLoop.timer()
    val endTime = initTime + atMost

    ManagedEventLoop.setBlocking
    @scala.annotation.tailrec
    def loop(f: Future[T]): Try[T] = {
      val execution: Duration = ManagedEventLoop.tick
      if(execution > endTime) Failure(new java.util.concurrent.TimeoutException(s"Futures timed out after [${atMost.toMillis}] milliseconds"))
      else f.value match {
        case None => loop(f)
        case Some(res) => res
      }
    }

    val ret = loop(f)
    ManagedEventLoop.resetBlocking
    ret match {
      case Success(m) => m
      // if it's a wrapped execution (something coming from inside a promise)
      // we need to unwrap it, otherwise just return the regular throwable
      case Failure(e) => throw {
        e match {
          case _: scala.concurrent.ExecutionException => e.getCause()
          case _ => e
        }
      }
    }
  }

  def result[T](awaitable: Awaitable[T], atMost: Duration): T =
    awaitable.result(atMost)(AwaitPermission)

}

class CyclicBarrier(val c: Int) {
  private var counter = c
  private var closed = Promise[Int]

  def countDown() = {
    counter -= 1
    if(counter == 0) closed.success(1)
  }
  def getCount() = counter
  def reset() = counter = c


  def await(timeout: Long, unit: TimeUnit): Boolean = {
    try {
      Await.result(closed.future, Duration.fromNanos(TimeUnit.NANOSECONDS.convert(timeout, unit)))
      true
    } catch {
      case e: Exception => throw e
    }
  }
}

class CountDownLatch(val c: Int) {
  private var counter = c
  private var closed = Promise[Int]

  def countDown() = {
    counter -= 1
    if(counter == 0) closed.success(1)
  }
  def getCount() = counter
  def reset() = counter = c


  def await(): Boolean = {
    try {
      Await.result(closed.future, 100 seconds)
      true
    } catch {
      case e: Exception => throw e
    }
  }

  def await(timeout: Long, unit: TimeUnit): Boolean = {
    try {
      Await.result(closed.future, Duration.fromNanos(TimeUnit.NANOSECONDS.convert(timeout, unit)))
      true
    } catch {
      case e: Exception => throw e
    }
  }
}
