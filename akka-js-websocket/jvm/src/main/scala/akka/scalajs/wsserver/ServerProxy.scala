package akka.scalajs.wsserver

import scala.collection.mutable
import scala.concurrent.Future

import akka.actor._
import akka.scalajs.wscommon._

import play.api.libs.json._
import play.api.libs.iteratee.Concurrent.Channel

import be.doeraene.spickling._
import be.doeraene.spickling.playjson._

object ServerProxyActor {
  case object SendEntryPointRef
}

class ServerProxyActor(out: ActorRef, entryPointRef: Future[ActorRef]) extends AbstractProxy {
  def this(out: ActorRef, entryPointRef: ActorRef) = this(out, Future.successful(entryPointRef))

  import AbstractProxy._
  import ServerProxyActor._

  type PickleType = JsValue
  implicit protected def pickleBuilder: PBuilder[PickleType] = PlayJsonPBuilder
  implicit protected def pickleReader: PReader[PickleType] = PlayJsonPReader

  private implicit def ec = context.dispatcher

  override def preStart() = {
    super.preStart()
    self ! SendEntryPointRef
  }

  override def postStop() = {
    self ! AbstractProxy.ConnectionClosed
    super.postStop()
    //channelToClient.end()
  }

  override def receive = super.receive.orElse[Any, Unit] {
    case SendEntryPointRef =>
      entryPointRef foreach { ref =>
        self ! SendToPeer(Welcome(ref))
      }
    case msg => self ! AbstractProxy.IncomingMessage(msg)
  }

  override protected def sendPickleToPeer(pickle: PickleType): Unit = {
    out ! pickle
  }
}

/*object ServerProxy {
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
    self ! SendEntryPointRef
  }

  override def postStop() = {
    super.postStop()
    channelToClient.end()
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
}*/
