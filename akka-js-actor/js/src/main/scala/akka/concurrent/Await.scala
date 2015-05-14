package akka.concurrent

import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

object BlockingEventLoop {
  import scala.scalajs.js
  import scala.scalajs.js.Dynamic.global
  import scala.scalajs.js.timers._
  import scala.collection.mutable.{ Queue, ListBuffer }
  
  private val oldSetTimeout = global.setTimeout
  private val oldSetInterval = global.setInterval
  private val oldClearTimeout = global.clearTimeout
  private val oldClearInterval = global.clearInterval
  
  private var isBlocking = false
  
  private def timer = js.Date.now()
  
  private val queue = new Queue[js.Function0[_]]
  private val timeoutEvents = new ListBuffer[(js.Function0[_], Double)]()
  private val intervalEvents = new ListBuffer[(js.Function0[_], Double)]()
  
  def wait(max: Duration): Unit = {
    import scala.scalajs.js
    val p = scala.concurrent.Promise[Int]
    
    val fn: js.Function0[Any] = { () =>
      p.success(0)
    }
    js.Dynamic.global.setTimeout(fn, max.toMillis)
    Await.result(p.future)
  }
  
  def switch() = {
    global.setTimeout = { (f: js.Function0[_], delay: Number) => 
      if(isBlocking)
        if(f.toString() != "undefined") {
          val handle =  f -> (timer + delay.doubleValue())
          timeoutEvents += handle
          handle.asInstanceOf[SetTimeoutHandle]
        }
      else oldSetTimeout(delay.asInstanceOf[js.Any]) {
        f
      } 
    }
    global.setInterval = { (f: js.Function0[_], interval: Number) => 
      if(isBlocking) {
        val handle = f -> interval.doubleValue()
        intervalEvents += handle
        handle.asInstanceOf[SetIntervalHandle]
      }
      else oldSetInterval(interval.asInstanceOf[js.Any]) {
        f
      }
    }
    global.clearTimeout = (handle: SetTimeoutHandle) => timeoutEvents -= handle.asInstanceOf[(js.Function0[_], Double)]
    global.clearInterval = (handle: SetIntervalHandle) => intervalEvents -= handle.asInstanceOf[(js.Function0[_], Double)]
  }
  
  def reset() = {
    assert(isEmpty)
    global.setTimeout = oldSetTimeout
    global.setInterval = oldSetInterval
    global.clearTimeout = oldClearTimeout
    global.clearInterval = oldClearInterval  
  }
  
  def tick(implicit lastRan: LastRan) = {
    timeoutEvents.filter(_._2 < timer).foreach({ x => 
      queue.enqueue(x._1)
      timeoutEvents -= x
    })
    
    intervalEvents.filter(_._2 < timer - lastRan.time).foreach({ x => 
      queue.enqueue(x._1)
    })
    
    if(queue.nonEmpty) queue.dequeue()()
  }
  
  def isEmpty = queue.isEmpty
  def blockingOn = isBlocking = true
  def blockingOff = isBlocking = false
  
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
    @scala.annotation.tailrec
    def loop(f: Future[T])(implicit lr: LastRan): T = {
      BlockingEventLoop.tick
      
      f.value match { 
        case None => loop(f)(LastRan(System.currentTimeMillis()))
        case Some(Success(m)) => m
        case Some(Failure(m)) => throw m
      }
    }

    loop(f)(LastRan(0L))
  }    
    
  def result[T](f: Future[T], atMost: Duration): T = {
    val futureDate = atMost.toMillis + System.currentTimeMillis()
    
    @scala.annotation.tailrec
    def loop(f: Future[T])(implicit lr: LastRan): T = {
      BlockingEventLoop.tick
      
      if(System.currentTimeMillis() >= futureDate) throw new java.util.concurrent.TimeoutException(s"Futures timed out after [${atMost.toMillis}] milliseconds")
      else f.value match { 
        case None => loop(f)(LastRan(System.currentTimeMillis()))
        case Some(Success(m)) => m
        case Some(Failure(m)) => throw m
      }
    }
    
    loop(f)(LastRan(0L))
  }
    
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