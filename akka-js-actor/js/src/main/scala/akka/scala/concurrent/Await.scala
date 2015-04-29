package akka.concurrent

import scala.concurrent.duration.Duration

object BlockingEventLoop {
  import scala.scalajs.js
  import scala.scalajs.js.Dynamic.global
  import scala.scalajs.js.timers._
  import scala.collection.mutable.{ Queue, ListBuffer }
  
  private val oldSetTimeout = global.setTimeout
  private val oldSetInterval = global.setInterval
  private val oldClearTimeout = global.clearTimeout
  private val oldClearInterval = global.clearInterval
  
  private def timer = js.Date.now()
  private var lastRun: Double = 0
  
  private val queue = new Queue[js.Function0[_]]
  private val timeoutEvents = new ListBuffer[(js.Function0[_], Double)]()
  private val intervalEvents = new ListBuffer[(js.Function0[_], Double)]()
  
  def switch = {
    global.setTimeout = { (f: js.Function0[_], delay: Number) => 
      val handle =  f -> (timer + delay.doubleValue())
      timeoutEvents += handle
      handle.asInstanceOf[SetTimeoutHandle]
    }
    global.setInterval = { (f: js.Function0[_], interval: Number) => 
      val handle = f -> interval.doubleValue()
      intervalEvents += handle
      handle.asInstanceOf[SetIntervalHandle]
    }
    global.clearTimeout = (handle: SetTimeoutHandle) => timeoutEvents -= handle.asInstanceOf[(js.Function0[_], Double)]
    global.clearInterval = (handle: SetIntervalHandle) => intervalEvents -= handle.asInstanceOf[(js.Function0[_], Double)]
  }
  
  def reset = {
    global.setTimeout = oldSetTimeout
    global.setInterval = oldSetInterval
    global.clearTimeout = oldClearTimeout
    global.clearInterval = oldClearInterval
  }
  
  def tick = {
    timeoutEvents.filter(_._2 < timer).foreach({ x => 
      queue.enqueue(x._1)
      timeoutEvents -= x
    })
    
    intervalEvents.filter(_._2 < timer - lastRun).foreach({ x => 
      queue.enqueue(x._1)
    })
    
    if(queue.nonEmpty) queue.dequeue()()
    lastRun = timer
  }
}

sealed trait CanAwait extends AnyRef 

trait Awaitable[T] {
   // def ready(atMost: Duration)(implicit permit: CanAwait): Awaitable.this.type 
   // def result(atMost: Duration)(implicit permit: CanAwait): T 
}

object Await {
  import scala.concurrent.Future
  import scala.util.{ Success, Failure }
  @scala.annotation.tailrec
  def result[A](f: Future[A]): A = {
    BlockingEventLoop.tick
    f.value match { 
      case None => result(f)
      case Some(Success(m)) => m
      case Some(Failure(m)) => throw m
    }
  }
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
  
  
  def await = Await.result(closed.future)
}