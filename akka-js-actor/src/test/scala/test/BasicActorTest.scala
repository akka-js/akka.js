package test

import akka.actor._
import utest._

import scala.concurrent._
import scala.collection.mutable.Queue
import scala.scalajs.js.Dynamic.global
import scala.scalajs.js

case class Greeting(who: String)

class GreetingActor extends Actor {
  def receive = {
    case Greeting(who) => {
      sender ! ("Hello " + who)
    }
  }
}

object BasicActorTest extends TestSuite {
  implicit val ec = utest.ExecutionContext.RunNow

  //def withActorSystem(body: ActorSystem => Future[_]) = {
    val eventQueue = new Queue[js.Function0[_]]

    val oldSetTimeout = global.setTimeout
    val oldClearTimeout = global.clearTimeout
    val oldSetInterval = global.setInterval
    val oldClearInterval = global.clearInterval

    var lastID = 0
    global.setTimeout = { (f: js.Function0[_], delay: Number) =>
      eventQueue.enqueue(f)
      lastID += 1
      lastID
    }
    global.clearTimeout = { () => sys.error("Stub for clearTimeout") }
    global.setInterval  = { () => sys.error("Stub for setInterval") }
    global.clearInterval = { () => sys.error("Stub for clearInterval") }

    val system = ActorSystem("greeting-system")

    /*val f = body(system)

    while (eventQueue.nonEmpty) {
      val event = eventQueue.dequeue()
      event()
    }

    global.setTimeout = oldSetTimeout
    global.clearTimeout = oldClearTimeout
    global.setInterval = oldSetInterval
    global.clearInterval = oldClearInterval

    f
  }*/

  def withActorSystem(body: ActorSystem => Future[_]) = {
    val f = body

    while (eventQueue.nonEmpty) {
      val event = eventQueue.dequeue()
      event()
    }

    global.setTimeout = oldSetTimeout
    global.clearTimeout = oldClearTimeout
    global.setInterval = oldSetInterval
    global.clearInterval = oldClearInterval

    f
  }

  val tests = TestSuite {
    "spawn an actor and send a message" - {
      withActorSystem { system =>
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

        p.future
      }
    }
  }
}

