package org.scalajs.actors
package test
package helloactors

import scala.scalajs.js
import js.Dynamic.global

case class Greeting(who: String)

class GreetingActor extends Actor {
  def receive = {
    case Greeting(who) => global.console.log("Hello " + who)
  }
}

object HelloActorsTest extends ActorsTest {
  global.console.log("Starting test")
  val system = ActorSystem("MySystem")
  val greeter = system.actorOf(Props(new GreetingActor), name = "greeter")
  greeter ! Greeting("Charlie Parker")
}
