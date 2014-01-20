package akka.scalajs.wsserver

import akka.actor._

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Akka

import scala.concurrent.ExecutionContext.Implicits._

object ActorWebSocket {
  def apply(props: Props)(implicit app: Application): WebSocket[JsValue] = {
    WebSocket.using[JsValue] { request =>
      // Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
      val (out, channel) = Concurrent.broadcast[JsValue]

      val serverProxy = Akka.system.actorOf(
          Props(classOf[ServerProxy], channel, props))

      // Forward incoming messages as messages to the proxy
      val in = Iteratee.foreach[JsValue] {
        msg => serverProxy ! ServerProxy.IncomingMessage(msg)
      }.map {
        _ => serverProxy ! ServerProxy.ConnectionClosed
      }

      (in, out)
    }
  }
}
