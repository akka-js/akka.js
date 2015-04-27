package test

//import akka.actor._
//import utest._

/**
import scala.language.postfixOps
import scala.concurrent._
import scala.concurrent.duration._
import scala.collection.mutable.Queue
import scala.scalajs.js.Dynamic.global
import scala.scalajs.js

import org.scalatest._
*/
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

/**
case class Greeting(who: String)

class GreetingActor extends Actor {
  
  override def preStart() = println("Greeter started")
  
  def receive = {
    case Greeting(who) => {
      println("YO")
      println(who)
      sender ! ("Hello " + who)
    }
  }
}

class Greeting2Actor(args: Seq[Any]) extends ExportableActor {
  
  val prefix = args(0).toString
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
      p.future
    }
    
    "spawn an actor with parameters" - {
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
    }
    
  }
}

*/