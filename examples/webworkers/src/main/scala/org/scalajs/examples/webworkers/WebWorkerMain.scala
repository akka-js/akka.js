package org.scalajs.examples.webworkers

import scala.scalajs.js
import js.annotation.JSExport
import js.Dynamic.global

import org.scalajs.spickling.PicklerRegistry
import akka.actor._
import akka.scalajs.webworkers.WebWorkerRouter

class GreetingActor extends Actor {
  def receive = {
    case Greeting(who) =>
      global.console.log("Hello " + who)
      sender ! Greeting("returning " + who)
  }
}

@JSExport
object WebWorkerMain {
  println("Starting WebWorkerMain")
  PicklerRegistry.register[Greeting]

  WebWorkerRouter.setupAsChild()

  var system: ActorSystem = _

  @JSExport
  def main(): Unit = {
    WebWorkerRouter.onInitialized {
      global.console.log("Worker initialized with address "+WebWorkerRouter.address)
      system = ActorSystem("WorkerSystem")
      val greeter = system.actorOf(Props(new GreetingActor), name = "greeter")
    }
  }
}
