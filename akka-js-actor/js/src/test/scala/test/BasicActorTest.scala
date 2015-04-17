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
    case Greeting(who) => sender ! ("Hello " + who)
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

      p.future
    }
  }
}

