package akka.scalajs.wsserver

import akka.actor._

import play.api.libs.json._
import play.api.libs.iteratee.Concurrent.Channel

object ServerProxy {
  case class IncomingMessage(msg: JsValue)
  case object ConnectionClosed
}

class ServerProxy(channelToClient: Channel[JsValue],
    entryPointProps: Props) extends Actor with ActorLogging {

  import ServerProxy._

  override def preStart() = {
    super.preStart()
    log.info(s"starting $self")
  }

  def receive = {
    case IncomingMessage(msg) =>
      // TODO For now we just echo back
      log.info(s"receiving message: $msg")
      channelToClient push msg

    case ConnectionClosed =>
      log.info(s"closing $self")
      context.stop(self)
  }
}
