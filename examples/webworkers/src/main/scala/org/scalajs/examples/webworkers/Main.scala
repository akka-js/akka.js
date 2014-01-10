package org.scalajs.examples.webworkers

import org.scalajs.actors._

object Main {
  val system = ActorSystem("MySystem")

  def main(): Unit = {
    val greeter = system.actorOf(Props(new GreetingActor), name = "greeter")
    greeter ! Greeting("Charlie Parker")
  }
}
