package org.scalajs.actors
package webworkers

import scala.scalajs.js
import org.scalajs.spickling.PicklerRegistry

private[actors] class WorkerActorRef(
    system: WebWorkersActorSystem,
    val path: ActorPath) extends ActorRef {

  private[this] val pickledPath = PicklerRegistry.pickle(path)

  def !(msg: Any)(implicit sender: ActorRef): Unit = {
    val messagePickle: js.Any = PicklerRegistry.pickle(msg)
    val senderID = sender match {
      case Actor.noSender => null
      case _ => system.globalizePath(sender.path)
    }
    val senderIDPickle: js.Any = PicklerRegistry.pickle(senderID)
    val data = js.Dynamic.literal(
        kind = "bang",
        sender = senderIDPickle,
        receiver = pickledPath,
        message = messagePickle)
    WebWorkerRouter.postMessageTo(path.address, data)
  }
}
