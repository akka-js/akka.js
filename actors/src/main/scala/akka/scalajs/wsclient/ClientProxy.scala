package akka.scalajs.wsclient

import akka.actor._
import akka.scalajs.wscommon._

import scala.scalajs.js
import akka.scalajs.jsapi._

import org.scalajs.spickling._
import org.scalajs.spickling.jsany._

object ClientProxy {
  case object ConnectionError
}

case class WebSocketConnected(entryPointRef: ActorRef)

class ClientProxy(wsUrl: String, connectedHandler: ActorRef) extends AbstractProxy {
  /** Will send the WebSocketConnected message to parent actor. */
  def this(wsUrl: String) = this(wsUrl, null)

  import AbstractProxy._
  import ClientProxy._

  type PickleType = js.Any
  implicit protected def pickleBuilder: PBuilder[PickleType] = JSPBuilder
  implicit protected def pickleReader: PReader[PickleType] = JSPReader

  var webSocket: WebSocket = _

  override def preStart() = {
    super.preStart()
    log.info(s"connecting to $wsUrl")

    webSocket = new WebSocket(wsUrl)
    webSocket.addEventListener("message", { (event: Event) =>
      self ! IncomingMessage(js.JSON.parse(
          event.asInstanceOf[MessageEvent].data.toString()))
    }, useCapture = false)
    webSocket.addEventListener("close", { (event: Event) =>
      self ! ConnectionClosed
    }, useCapture = false)
    webSocket.addEventListener("error", { (event: Event) =>
      self ! ConnectionError
    }, useCapture = false)
  }

  override def receive = super.receive.orElse[Any, Unit] {
    case ConnectionError =>
      throw new akka.AkkaException("WebSocket connection error")
  }

  override def receiveFromPeer = super.receiveFromPeer.orElse[Any, Unit] {
    case Welcome(entryPointRef) =>
      val msg = WebSocketConnected(entryPointRef)
      if (connectedHandler eq null) context.parent ! msg
      else connectedHandler ! msg
  }

  override protected def sendPickleToPeer(pickle: PickleType): Unit = {
    webSocket.send(js.JSON.stringify(pickle))
  }

}
