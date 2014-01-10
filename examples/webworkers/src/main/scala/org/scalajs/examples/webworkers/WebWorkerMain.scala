package org.scalajs.examples.webworkers

import scala.scalajs.js
import js.Dynamic.global

import org.scalajs.actors._

class GreetingActor extends Actor {
  def receive = {
    case Greeting(who) => global.console.log("Hello " + who)
  }
}

object WebWorkerMain {
  val system = ActorSystem("WebWorkerSystem")

  ParentWebWorkerConnection.onmessage = { (event: MessageEvent) =>
    val data = event.data
    if (!(!data.isActorSystemMessage))
      handleActorSystemMessage(data)
  }

  def main(): Unit = {
    val greeter = system.actorOf(Props(new GreetingActor), name = "greeter")
  }

  def handleActorSystemMessage(data: js.Dynamic): Unit = {

  }
}
