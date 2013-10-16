package ch.epfl.lamp.scalajs.actors.test.hello

import ch.epfl.lamp.scalajs.actors._

import scala.js.Dynamic.global

case class Greeting(who: String)

class GreetingActor extends Actor {
  def receive = {
    case Greeting(who) => global.console.log("Hello " + who)
  }
}

object HelloActors {
  def main(): Unit = {
    val system = ActorSystem("MySystem")
    val greeter = system.actorOf(Props(new GreetingActor), name = "greeter")
    greeter ! Greeting("Charlie Parker")
  }
}
