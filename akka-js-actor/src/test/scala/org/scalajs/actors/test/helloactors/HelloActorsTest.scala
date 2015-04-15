package org.scalajs.actors.test
package helloactors

import scala.scalajs.js
import js.Dynamic.global

import akka.actor._

case class Greeting(who: String)

class GreetingActor extends Actor {
  def receive = {
    case Greeting(who) => global.console.log("Hello " + who)
  }
}

object HelloActorsTest extends ActorsTest {
  global.console.log("Starting test")
  val system = ActorSystem("MySystem")
  global.console.log("Actor system created")
  val greeter = system.actorOf(Props(new GreetingActor), name = "greeter")
  global.console.log("Actor created")
  greeter ! Greeting("Charlie Parker")
}
