package akka.scalajs.webworkers

import scala.scalajs.js
import org.scalajs.spickling._
import org.scalajs.spickling.jsany._

import akka.actor._

private[akka] object WebWorkersActorSystem {
  private var _registrationsDone = false

  case class SendMessage(msg: Any, receiver: ActorRef, sender: ActorRef)

  def registerPicklers(): Unit = {
    if (_registrationsDone)
      return
    _registrationsDone = true

    import PicklerRegistry.register
    import DedicatedPicklers._

    register[Some[Any]]
    register(None)

    register[RootActorPath]
    register[ChildActorPath]
    register[Address]

    register[SendMessage]
  }
}

private[akka] trait WebWorkersActorSystem extends ActorSystem { this: ActorSystemImpl =>
  import WebWorkersActorSystem._

  registerPicklers()

  WebWorkerRouter.registerSystem(name, this)

  private[webworkers] def workerAddress = {
    Address(name, WebWorkerRouter.address)
  }

  private[webworkers] def globalizePath(path: ActorPath): ActorPath = {
    if (path.address.hasGlobalScope) path
    else RootActorPath(workerAddress) / path.elements
  }

  private[webworkers] def sendMessageAcrossWorkers(destWorker: Address,
      msg: Any, receiver: ActorRef, sender: ActorRef): Unit = {
    WebWorkerRouter.postMessageTo(destWorker,
        picklerRegistry.pickle(SendMessage(msg, receiver, sender)))
  }

  // Handling messages arriving to my webworker

  private[webworkers] def deliverMessageFromRouter(pickle: js.Dynamic): Unit = {
    picklerRegistry.unpickle[js.Any](pickle) match {
      case SendMessage(msg, receiver, sender) =>
        receiver.tell(msg, sender)
    }
  }

  /** Pickler registry which knows how to deal with ActorRefs. */
  object picklerRegistry extends PicklerRegistry {
    def pickle[P](value: Any)(implicit builder: PBuilder[P],
        registry: PicklerRegistry): P = {
      value match {
        case ref: ActorRef =>
          val globalPath = globalizePath(ref.path)
          builder.makeObject(("ref", registry.pickle(globalPath)))

        case _ =>
          PicklerRegistry.pickle(value)
      }
    }

    def unpickle[P](pickle: P)(implicit reader: PReader[P],
        registry: PicklerRegistry): Any = {
      pickle match {
        case null => null

        case _ if !reader.isUndefined(reader.readObjectField(pickle, "ref")) =>
          val globalPath = registry.unpickle(reader.readObjectField(
              pickle, "ref")).asInstanceOf[ActorPath]
          if (globalPath.address == workerAddress)
            resolveLocalActorPath(globalPath).getOrElse(deadLetters)
          else
            new WorkerActorRef(WebWorkersActorSystem.this, globalPath)

        case _ =>
          PicklerRegistry.unpickle(pickle)
      }
    }
  }
}
