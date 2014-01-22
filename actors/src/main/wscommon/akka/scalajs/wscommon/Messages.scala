package akka.scalajs.wscommon

import akka.actor._
import akka.dispatch.sysmsg.SystemMessage

@SerialVersionUID(1L)
case class Welcome(entryPoint: ActorRef) extends Serializable

@SerialVersionUID(1L)
case class SendMessage(msg: Any, receiver: ActorRef, sender: ActorRef) extends Serializable

@SerialVersionUID(1L)
case class ForeignTerminated(ref: ActorRef) extends Serializable
