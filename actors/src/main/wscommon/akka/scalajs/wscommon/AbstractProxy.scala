package akka.scalajs.wscommon

import scala.collection.mutable

import akka.actor._
import akka.event.LoggingReceive
import akka.scalajs.wscommon._

import org.scalajs.spickling._

object AbstractProxy {
  case class IncomingMessage(pickle: Any) // JsValue/js.Any = P
  case object ConnectionClosed
  case class SendToPeer(message: Any)

  private lazy val _registerPicklers: Unit = {
    import PicklerRegistry.register
    register[Welcome]
    register[SendMessage]
    register[ForeignTerminated]
  }

  private def registerPicklers(): Unit = _registerPicklers
}

/** Common between [[akka.scalajs.wsserver.ServerProxy]] and
 *  [[akka.scalajs.wsclient.ClientProxy]].
 */
abstract class AbstractProxy extends Actor with ActorLogging {

  import AbstractProxy._

  type PickleType
  implicit protected def pickleBuilder: PBuilder[PickleType]
  implicit protected def pickleReader: PReader[PickleType]

  registerPicklers()
  protected val picklerRegistry = new ActorRefAwarePicklerRegistry(this)

  private[this] var _nextLocalID: Long = 0
  private def nextLocalID(): String = {
    _nextLocalID += 1
    _nextLocalID.toString
  }

  private val localIDs = mutable.Map.empty[ActorRef, String]
  private val localIDsRev = mutable.Map.empty[String, ActorRef]

  private val foreignIDs = mutable.Map.empty[ActorRef, String]
  private val foreignIDsRev = mutable.Map.empty[String, ActorRef]

  def receive = {
    case IncomingMessage(pickle) =>
      log.info(s"IncomingMessage($pickle)")
      val msg = picklerRegistry.unpickle(pickle.asInstanceOf[PickleType])
      log.info(s"  -> unpickled: $msg")
      receiveFromPeer(msg)

    case ConnectionClosed =>
      log.info(s"closing $self")
      context.stop(self)

    case SendToPeer(message) =>
      sendToPeer(message)

    case Terminated(ref) =>
      localIDs.remove(ref).foreach(localIDsRev -= _)
      foreignIDs -= ref
  }

  protected def receiveFromPeer: Receive = LoggingReceive {
    case m @ SendMessage(message, receiver, sender) =>
      log.info(s"receiveFromPeer: $m")
      receiver.tell(message, sender)

    case ForeignTerminated(ref) =>
      context.stop(ref)
  }

  protected def sendToPeer(msg: Any): Unit = {
    log.debug(s"sendToClient($msg)")
    val pickle = picklerRegistry.pickle(msg)
    log.debug(s"  -> pickled: $pickle")
    sendPickleToPeer(pickle)
  }

  protected def sendPickleToPeer(pickle: PickleType): Unit

  private[wscommon] def pickleActorRef[P](ref: ActorRef)(
      implicit builder: PBuilder[P]): P = {
    val (side, id) = if (context.children.exists(_ == ref)) {
      /* This is a proxy actor for an actor on the client.
       * We need to unbox it to recover the ID the client gave to us for it.
       */
      ("receiver", foreignIDs(ref))
    } else {
      /* This is an actor on the server (or somewhere else).
       * The client will have to make a proxy for this one with an ID we choose.
       */
      val id = localIDs.getOrElseUpdate(ref, {
        context.watch(ref)
        val id = nextLocalID()
        localIDsRev += id -> ref
        id
      })
      ("sender", id)
    }
    builder.makeObject(
        ("side", builder.makeString(side)),
        ("id", builder.makeString(id)))
  }

  private[wscommon] def unpickleActorRef[P](pickle: P)(
      implicit reader: PReader[P]): ActorRef = {
    val side = reader.readString(reader.readObjectField(pickle, "side"))
    val id = reader.readString(reader.readObjectField(pickle, "id"))

    side match {
      case "receiver" =>
        localIDsRev(id)

      case "sender" =>
        foreignIDsRev.getOrElse(id, {
          // we don't have a proxy yet, make one
          val ref = context.watch(context.actorOf(Props(new ForeignActorProxy)))
          foreignIDs += ref -> id
          foreignIDsRev += id -> ref
          ref
        })
    }
  }
}

class ForeignActorProxy extends Actor {
  import AbstractProxy._

  def receive = {
    case message =>
      context.parent ! SendToPeer(SendMessage(message, self, sender))
  }
}

/** My pickler registry with hooks for pickling and unpickling ActorRefs. */
class ActorRefAwarePicklerRegistry(proxy: AbstractProxy) extends PicklerRegistry {
  val base = PicklerRegistry

  override def pickle[P](value: Any)(implicit builder: PBuilder[P],
      registry: PicklerRegistry): P = {
    value match {
      case ref: ActorRef => builder.makeObject(("ref", proxy.pickleActorRef(ref)))
      case _             => base.pickle(value)
    }
  }

  override def unpickle[P](pickle: P)(implicit reader: PReader[P],
      registry: PicklerRegistry): Any = {
    if (reader.isNull(pickle)) {
      null
    } else {
      val refData = reader.readObjectField(pickle, "ref")
      if (!reader.isUndefined(refData))
        proxy.unpickleActorRef(refData)
      else
        base.unpickle(pickle)
    }
  }
}
