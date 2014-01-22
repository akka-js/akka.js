package akka.scalajs.wsserver

import scala.collection.mutable
import scala.concurrent.Future

import akka.actor._
import akka.scalajs.wscommon._

import play.api.libs.json._
import play.api.libs.iteratee.Concurrent.Channel

import org.scalajs.spickling._
import org.scalajs.spickling.playjson._

object ServerProxy {
  case object SendEntryPointRef
}

class ServerProxy(channelToClient: Channel[JsValue],
    entryPointRef: Future[ActorRef]) extends AbstractProxy {
  def this(channelToClient: Channel[JsValue],
    entryPointRef: ActorRef) = this(channelToClient, Future.successful(entryPointRef))

  import AbstractProxy._
  import ServerProxy._

  type PickleType = JsValue
  implicit protected def pickleBuilder: PBuilder[PickleType] = PlayJsonPBuilder
  implicit protected def pickleReader: PReader[PickleType] = PlayJsonPReader

  private implicit def ec = context.dispatcher

  override def preStart() = {
    super.preStart()
    log.info(s"starting $self")
    self ! SendEntryPointRef
  }

  override def receive = super.receive.orElse[Any, Unit] {
    case SendEntryPointRef =>
      entryPointRef foreach { ref =>
        self ! SendToPeer(Welcome(ref))
      }
  }

  override protected def sendPickleToPeer(pickle: PickleType): Unit = {
    channelToClient push pickle
  }
}
