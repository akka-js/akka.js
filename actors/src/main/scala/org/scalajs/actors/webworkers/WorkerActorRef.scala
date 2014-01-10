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
      case Actor.noSender => ""
      case _ => system.globalizePath(sender.path)
    }
    val senderIDPickle: js.Any = PicklerRegistry.pickle(senderID)
    val data = js.Dynamic.literal(
        isActorSystemMessage = true,
        kind = "bang",
        senderPath = senderIDPickle,
        receiverPath = pickledPath,
        message = messagePickle)
    val nextHop = system.computeNextHop(path.address)
    nextHop.get.postMessage(data)
  }
}
