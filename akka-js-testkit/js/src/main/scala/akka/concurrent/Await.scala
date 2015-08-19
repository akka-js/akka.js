package akka.concurrent

import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.scalajs.runtime.UndefinedBehaviorError
import scala.util.Try

object ManagedEventLoop {
  import scala.scalajs.js
  import scala.scalajs.js.Dynamic.global
  import scala.scalajs.js.timers._
  import scala.collection.mutable.{ Queue, ListBuffer }

  private val jsSetTimeout = global.setTimeout
  private val jsSetInterval = global.setInterval
  private val jsClearTimeout = global.clearTimeout
  private val jsClearInterval = global.clearInterval

  def timer() = Duration.fromNanos(System.nanoTime())

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
  private final case class TimeoutEvent(handler: JSFun, time: Double) extends Event(handler) {
    def clear(): Unit = externalQueueH.map(x => jsClearTimeout(x))
    def globalize(): Unit =
      externalQueueH = Some(
        jsSetTimeout(() => {
          handler()
          events -= this
        }, time.asInstanceOf[js.Any])
      )
    def hasToRun(now: Duration): Boolean = {
      //println("now "+now )
      //println("creation" +creationDate)
      //println("time "+time)
      //println("result "+((now - creationDate) > time))
      //println("has to run -> "+(now > creationDate + Duration.fromNanos(time*1000000)))
      (now > creationDate + Duration.fromNanos(time*1000000))
    }
    val hasToBeRemoved: Boolean = true
  }
  private final case class IntervalEvent(handler: JSFun, time: Double) extends Event(handler) {
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
      (now > lastRan + Duration.fromNanos(time*1000000))
      //(now - lastRan) > (time*1000000)
    }
    val hasToBeRemoved: Boolean = false
  }

  private val events = new ListBuffer[Event]()

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

    //println("before")
    val toBeProcessed = events.filter(_.hasToRun(now))
    //println("asfter")

    toBeProcessed.foreach(_.run())

    //if (toBeProcessed.length > 0)
      //println("\n\n\n RUN \n\n\n\n")

    val toBeRemoved = toBeProcessed.filter(_.hasToBeRemoved)

    //println("toBeProcessed "+toBeProcessed)
    //println("toBeRemoved "+toBeRemoved)
    //println("events.length before "+events.length)
    events --= toBeRemoved
    //println("events.length after "+events.length)

    now
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


  manage
}

object BlockingEventLoop {
  /*
  self =>
  import scala.scalajs.js
  import scala.scalajs.js.Dynamic.global
  import scala.scalajs.js.timers._
  import scala.collection.mutable.{ Queue, ListBuffer }

  private val oldSetTimeout = global.setTimeout
  private val oldSetInterval = global.setInterval
  private val oldClearTimeout = global.clearTimeout
  private val oldClearInterval = global.clearInterval

  def timer = js.Date.now()

  //private val queue = new Queue[js.Function0[_]]
  private val timeoutEvents = new ListBuffer[(js.Function0[_], Double)]()
  private val intervalEvents = new ListBuffer[((js.Function0[_], Double), Double)]()
*/
  var isBlocking = false

  def wait(max: Duration): Unit = {
    import scala.scalajs.js
    val p = scala.concurrent.Promise[Int]

    val fn: js.Function0[Any] = { () =>
      p.success(0)
    }
    js.Dynamic.global.setTimeout(fn, max.toMillis)
    Await.result(p.future)
  }

  def aWhile(cond: => Boolean)(b: => Unit) = {
    import scala.scalajs.js
    import scala.scalajs.js.Dynamic.global

    val p = scala.concurrent.Promise[Boolean]
    lazy val fn: js.Function0[Any] = { () =>
      if(cond) {
        b
        global.setTimeout(fn, 100)
      } else p.success(true)
    }

    global.setTimeout(fn, 100)

    Await.result(p.future)
  }

  def switch(): Unit = switch(false)

  def switch(isBlocking: Boolean): Unit = {
    /*
    self.isBlocking = isBlocking
    if (isBlocking) {
      global.setTimeout = { (f: js.Function0[_], delay: Double) =>
          if(f.toString() != "undefined") {
            val handle =  f -> (timer + delay.doubleValue())
            timeoutEvents += handle
            handle.asInstanceOf[SetTimeoutHandle]
          }
      }
      global.setInterval = { (f: js.Function0[_], interval: Double) =>
          val handle = f -> timer
          intervalEvents +=( (handle, timer) )
          handle.asInstanceOf[SetIntervalHandle]
      }
      global.clearTimeout = (handle: SetTimeoutHandle) =>
        try {
          timeoutEvents -= handle.asInstanceOf[(js.Function0[_], Double)] }
        catch { case _: UndefinedBehaviorError => oldClearTimeout(handle) }
      global.clearInterval = (handle: SetIntervalHandle) =>
        try {
          val events = intervalEvents.filter(x => x._1 == handle.asInstanceOf[(js.Function0[_], Double)])
          intervalEvents --= events
          //intervalEvents -= handle.asInstanceOf[(js.Function0[_], Double)]
        } catch { case _: UndefinedBehaviorError => oldClearInterval(handle) }
    } else
      reset
      */
  }

  def reset() = {
    //assert(isEmpty)
    /*
    global.setTimeout = oldSetTimeout
    global.setInterval = oldSetInterval
    global.clearTimeout = oldClearTimeout
    global.clearInterval = oldClearInterval
    */
  }


  def tick(implicit lastRan: LastRan) = {
    /*
    val now = timer

    val toBeRunned = timeoutEvents.filter(_._2 >= now)//.filter(_._2 <= now)
    toBeRunned.foreach(x => x._1()/*queue.enqueue(x._1)*/)
    timeoutEvents --= toBeRunned

    //this is not good, we need another implementation
    //intervalEvents.filter(_._2 /*<=*/>= (now - lastRan.time)).foreach({ x =>
      //queue.enqueue(x._1)

    //})
    val toBeUpdated =
      intervalEvents.filter{
        case ((fun, timer), lastTime) =>
          if ((now - lastTime) >= timer) {
            fun()
            true
          } else false

      }

    intervalEvents --= toBeUpdated
    intervalEvents ++=(toBeUpdated.map(x => (x._1, now)))

    //while(queue.nonEmpty) queue.dequeue()()

    now
    */
  }

  //def isEmpty = queue.isEmpty
  def blockingOn = switch(isBlocking = true)
  def blockingOff = switch(isBlocking = false)

  switch()
}

sealed trait CanAwait extends AnyRef

object AwaitPermission extends CanAwait

trait Awaitable[T] {
   def ready(atMost: Duration)(implicit permit: CanAwait): Awaitable.this.type
   def result(atMost: Duration)(implicit permit: CanAwait): T
}

case class LastRan(time: Double)

object Await {
  import scala.concurrent.Future
  import scala.scalajs.js.Date
  import scala.util.{ Success, Failure }

  def ready[T](awaitable: Awaitable[T], atMost: Duration): awaitable.type =
    awaitable.ready(atMost)(AwaitPermission)

  def result[T](f: Future[T]): T = {
    import scala.concurrent.duration._
    result[T](f, 20 seconds/*Duration.Inf*/)
 }
/*
  def result[T](f: Future[T]): T = {
    @scala.annotation.tailrec
    def loop(f: Future[T])(implicit lr: LastRan): T = {
      val now = BlockingEventLoop.tick

      f.value match {
        case None => loop(f)(LastRan(now))
        case Some(Success(m)) => m
        case Some(Failure(m)) => throw m
      }
    }

    loop(f)(LastRan(0L))
  }
*/
  def result[T](f: Future[T], atMost: Duration): T = {
    val initTime = ManagedEventLoop.timer()
    val endTime = initTime + atMost

    ManagedEventLoop.setBlocking
    @scala.annotation.tailrec
    def loop(f: Future[T]): Try[T] = {
      val execution: Duration = ManagedEventLoop.tick

      if(execution > endTime) throw new java.util.concurrent.TimeoutException(s"Futures timed out after [${atMost.toMillis}] milliseconds")
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
      case Failure(e) => throw (e match {
        case _: scala.concurrent.ExecutionException => e.getCause()
        case _ => e
      })
    }
  }
/*
  def result[T](f: Future[T], atMost: Duration): T = {
    val outerNow = BlockingEventLoop.timer
    val futureDate = atMost.toMillis + outerNow

    @scala.annotation.tailrec
    def loop(f: Future[T])(implicit lr: LastRan): T = {
      val now = BlockingEventLoop.tick

      if(now > futureDate) {
        //cancelTimeout
        throw new java.util.concurrent.TimeoutException(s"Futures timed out after [${atMost.toMillis}] milliseconds")
      } else f.value match {
        case None => loop(f)(LastRan(now))
        case Some(Success(m)) => m
        case Some(Failure(m)) => throw m
      }
    }

    loop(f)(LastRan(0L))
  }
*/
  def result[T](awaitable: Awaitable[T], atMost: Duration): T =
    awaitable.result(atMost)(AwaitPermission)

}

class CountDownLatch(val c: Int) {
  import scala.concurrent.Promise
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
