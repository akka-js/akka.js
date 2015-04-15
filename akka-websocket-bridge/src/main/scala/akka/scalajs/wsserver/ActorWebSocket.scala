package akka.scalajs.wsserver

import akka.actor._
import akka.scalajs.wscommon.AbstractProxy

import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Akka

import scala.concurrent.Future
//import scala.concurrent.ExecutionContext.Implicits._

import play.api.mvc.WebSocket
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

object MyActor {
  //def props(out: ActorRef) = Props(new MyActor(out))
}

class MyActor(out: ActorRef, entryPointRef: ActorRef) extends Actor {
  val serverProxy = context.actorOf(
    Props(classOf[ServerProxyActor], out, Future.successful(entryPointRef)))

  def receive = {
    case msg => serverProxy ! AbstractProxy.IncomingMessage(msg)
  }

  override def postStop() = {
    serverProxy ! AbstractProxy.ConnectionClosed
  }
}

object ActorWebSocket {
  /*def socket = WebSocket.using[JsValue] { request =>

    val (out, channel) = Concurrent.broadcast[String]
    val serverProxy = context.actorOf(
        Props(classOf[ServerProxy], channel, entryPointRef))

    val in = Iteratee.foreach[JsValue] {
    	msg =>
    	  	serverProxy ! AbstractProxy.IncomingMessage(msg)
    		channel push("I received your message: " + msg)
    }
    (in,out)
  }*/

  /*def apply(f: RequestHeader => Future[Any]) = {
    WebSocket.async[JsValue] { request =>
      f(request).map(_.asInstanceOf[(Iteratee[JsValue, Unit], Enumerator[JsValue])])
    }
  }*/

  def actorForWebSocketHandler(out: ActorRef, entryPointRef: ActorRef)(
    implicit context: ActorRefFactory): Props = {

    /*val serverProxy = context.actorOf(
      Props(classOf[MyActor], out, entryPointRef))*/
    val serverProxy = Props(classOf[MyActor], out, entryPointRef)
    // Forward incoming messages as messages to the proxy
    serverProxy
  }
/*  def actorForWebSocketHandler(entryPointRef: ActorRef)(
      implicit context: ActorRefFactory): (Iteratee[JsValue, Unit], Enumerator[JsValue]) = {

    val (out, channel) = Concurrent.broadcast[JsValue]
    val serverProxy = context.actorOf(
        Props(classOf[ServerProxy], channel, entryPointRef))

    // Forward incoming messages as messages to the proxy
    val in = Iteratee.foreach[JsValue] {
      msg => serverProxy ! AbstractProxy.IncomingMessage(msg)
    }.map {
      _ => serverProxy ! AbstractProxy.ConnectionClosed
    }

    (in, out)
  }*/
}
