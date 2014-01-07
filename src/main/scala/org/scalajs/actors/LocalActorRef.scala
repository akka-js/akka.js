package org.scalajs.actors

import scala.scalajs.js

private[actors] class LocalActorRef(
    _system: ActorSystem,
    _props: Props) extends ActorRef {

  val actorCell: ActorCell = new ActorCell(_system, _props, this, null)
  actorCell.create()

  def !(msg: Any)(implicit sender: ActorRef): Unit =
    sendMessage(Envelope(msg, sender, _system))

  protected def sendMessage(msg: Envelope): Unit = {
    js.Dynamic.global.setTimeout({ () =>
      dispatchNow(msg)
    }, 0)
  }

  protected def dispatchNow(msg: Envelope): Unit = {
    actorCell.invoke(msg)
  }
}
