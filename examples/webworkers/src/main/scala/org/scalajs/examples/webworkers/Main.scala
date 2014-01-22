package org.scalajs.examples.webworkers

import scala.scalajs.js
import js.Dynamic.global

import org.scalajs.spickling.PicklerRegistry
import akka.actor._
import akka.scalajs.webworkers.WebWorkerRouter

case object Start

class GreetingResponseActor extends Actor {
  def receive = {
    case Start =>
      global.console.log("Start")
      val root = RootActorPath(Address("WorkerSystem", Main.workerAddress))
      val path = root / "user" / "greeter"
      context.system.sendToPath(path, Greeting("John Parker"))

    case Greeting(who) =>
      global.console.log("Receiving back " + who)
      global.console.log("Wow! We made it back here! That's amazing!")
  }
}

object Main {
  PicklerRegistry.register(Start)
  PicklerRegistry.register[Greeting]

  WebWorkerRouter.initializeAsRoot()
  global.console.log("Will now create worker")
  val workerAddress = WebWorkerRouter.createChild("worker.js")

  val system = ActorSystem("MainSystem")

  def main(): Unit = {
    val greeter = system.actorOf(Props(new GreetingResponseActor),
        name = "greetingresponse")
    greeter ! Start
  }
}
