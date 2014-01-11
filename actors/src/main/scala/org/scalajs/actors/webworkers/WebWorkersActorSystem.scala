package org.scalajs.actors
package webworkers

import scala.scalajs.js
import org.scalajs.spickling.PicklerRegistry

private[actors] object WebWorkersActorSystem {
  private var _registrationsDone = false

  def registerPicklers(): Unit = {
    if (_registrationsDone)
      return
    _registrationsDone = true

    import PicklerRegistry.register

    register[Some[Any]]
    register(None)

    register[RootActorPath]
    register[ChildActorPath]
    register[Address]
  }
}

private[actors] trait WebWorkersActorSystem { this: ActorSystemImpl =>
  WebWorkersActorSystem.registerPicklers()

  WebWorkerRouter.registerSystem(name, this)

  private[webworkers] def workerAddress = {
    Address(name, WebWorkerRouter.address)
  }

  private[webworkers] def globalizePath(path: ActorPath): ActorPath = {
    if (path.address.hasGlobalScope) path
    else RootActorPath(workerAddress) / path.elements
  }

  // Handling messages arriving to my webworker

  private[webworkers] def deliverMessageFromRouter(message: js.Dynamic): Unit = {
    message.kind.toString match {
      case "bang" =>
        val senderPathPickle = message.sender
        val receiverPathPickle = message.receiver
        val messagePickle = message.message

        val senderPath =
          PicklerRegistry.unpickle(senderPathPickle).asInstanceOf[ActorPath]
        println(s"senderPath = $senderPath")
        val sender =
          if (senderPath eq null) null
          else new WorkerActorRef(this, senderPath)

        val receiverPath =
          PicklerRegistry.unpickle(receiverPathPickle).asInstanceOf[ActorPath]
        println(s"receiverPath = $receiverPath")
        val receiver = resolveLocalActorPath(receiverPath)
        println(s"receiver = $receiver")

        receiver foreach { ref =>
          val msg = PicklerRegistry.unpickle(messagePickle)
          println(s"msg = $msg")
          ref.!(msg)(sender)
        }
    }
  }
}
