package test

import akka.actor._
import utest._

import scala.language.postfixOps
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.Queue
import scala.scalajs.js.Dynamic.global
import scala.scalajs.js
import akka.event.Logging

//import org.scalatest._

/*
import collection.mutable.Stack
import org.scalatest._

class ExampleSpec extends FlatSpec with Matchers {

  "A Stack" should "pop values in last-in-first-out order" in {
    val stack = new Stack[Int]
    stack.push(1)
    stack.push(2)
    stack.pop() should be (2)
    stack.pop() should be (1)
  }

  it should "throw NoSuchElementException if an empty stack is popped" in {
    val emptyStack = new Stack[Int]
    a [NoSuchElementException] should be thrownBy {
      emptyStack.pop()
    } 
  }
}
*/

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

case class Greeting(who: String)

class GreetingActor extends Actor {
  val log = Logging(context.system, this)
  override def preStart() = println("Greeter started")
  
  def receive = {
    case Greeting(who) => {
      println("YO")
      println(who)
      sender ! ("Hello " + who)
    }
  }
}

class Greeting2Actor(prefix: String/*args: Seq[Any]*/) extends ExportableActor {
  
  //val prefix = args(0).toString
  def receive = {
    case Greeting(who) => {
      println(s"my prefix is $prefix")
      println(who)
      sender ! ("Hello " + who+ " I'm "+ prefix)
    }
  }
}

object BasicActorTest extends TestSuite {
  implicit val ec = utest.ExecutionContext.RunNow


  val tests = TestSuite {
    "spawn an actor and send a message" - {
      BlockingEventLoop.switch
      val system = ActorSystem("greeting-system")
      val actor = system.actorOf(Props(new GreetingActor), name = "greeter")

      val p = Promise[Int]

      val other = system.actorOf(Props(new Actor {
        def receive = {
          case "go" => actor ! Greeting("Bob")
          case "Hello Bob" => p.success(1)
          case _ => p.failure(new Exception("Doesn't match"))
        }
      }))

      other ! "go"

      system.scheduler.scheduleOnce(2 seconds)(p.tryFailure(new TimeoutException("too late")))
      val v = Await.result(p.future)
      BlockingEventLoop.reset
      assert(v == 1)
    }
    
    /*"spawn an actor with parameters" - {
      val system = ActorSystem("greeting2-system")
      println("Class is "+classOf[Greeting2Actor])
      val actor = system.actorOf(Props(classOf[Greeting2Actor], "Rob"), name = "greeter2")

      val p = Promise[Int]
      
      val other = system.actorOf(Props(new Actor {
        def receive = {
          case "go" => actor ! Greeting("Bob")
          case "Hello Bob I'm Rob" => p.success(1)
          case _ => p.failure(new Exception("Doesn't match"))
        }
      }))

      other ! "go"

      system.scheduler.scheduleOnce(2 seconds)(p.tryFailure(new TimeoutException("too late")))
      p.future
    }*/
    
  }
}