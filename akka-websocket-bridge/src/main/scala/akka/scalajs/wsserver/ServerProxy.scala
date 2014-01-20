package akka.scalajs.wsserver

import akka.actor._

import play.api.libs.json._
import play.api.libs.iteratee.Concurrent.Channel

import org.scalajs.spickling.PicklerRegistry
import org.scalajs.spickling.playjson._

object ServerProxy {
  case class IncomingMessage(msg: JsValue)
  case object ConnectionClosed
}

case class Person(name: String, age: Int)

class ServerProxy(channelToClient: Channel[JsValue],
    entryPointProps: Props) extends Actor with ActorLogging {

  import ServerProxy._

  PicklerRegistry.register[Person]

  override def preStart() = {
    super.preStart()
    log.info(s"starting $self")
  }

  def receive = {
    case IncomingMessage(msg) =>
      // TODO For now we just echo back
      log.info(s"receiving message: $msg")
      val value = PicklerRegistry.unpickle(msg)
      log.info(s"unpickled is: $value")
      val pickled = PicklerRegistry.pickle(value)
      log.info(s"repickled is: $pickled")
      channelToClient push pickled

    case ConnectionClosed =>
      log.info(s"closing $self")
      context.stop(self)
  }
}
