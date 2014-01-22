package client

import akka.actor._
import akka.scalajs.wsclient._

import models._

import org.scalajs.jquery.{jQuery => jQ, _}

object Main {
  RegisterPicklers.registerPicklers()

  val system = ActorSystem("chat-client")
  val manager = system.actorOf(Props(new Manager))

  def startup(): Unit = {
    jQ("#connect-button") click { (event: JQueryEventObject) => connect }
    jQ("#disconnect-button") click { (event: JQueryEventObject) => disconnect }
    jQ("#send-button") click { (event: JQueryEventObject) => send }
  }

  def connect(): Unit = {
    val nickname = jQ("#nickname-edit").value().toString()
    val user = User(nickname)
    manager ! ConnectAs(user)
  }

  def disconnect(): Unit = {
    manager ! Disconnect
  }

  def send(): Unit = {
    val text = jQ("#msg-to-send").value().toString()
    manager ! Send(text)
    jQ("#msg-to-send").value("")
  }
}

case class ConnectAs(user: User)
case class Send(text: String)
case object Disconnect
case object Disconnected

class Manager extends Actor {
  val proxyManager = context.actorOf(Props(new ProxyManager))
  var user: User = User("<noone>")
  var service: ActorRef = context.system.deadLetters

  def receive = {
    case m @ ConnectAs(user) =>
      this.user = user
      jQ("#connect-button").text("Connecting ...").prop("disabled", true)
      jQ("#nickname-edit").prop("disabled", true)
      proxyManager ! m

    case m @ WebSocketConnected(entryPoint) =>
      service = entryPoint
      service ! Join(user)
      jQ("#status-disconnected").addClass("status-hidden")
      jQ("#status-connected").removeClass("status-hidden")
      jQ("#nickname").text(user.nick)
      jQ("#send-button").prop("disabled", false)
      jQ("#disconnect-button").text("Disconnect").prop("disabled", false)

    case Send(text) =>
      val message = Message(user, text, System.currentTimeMillis())
      service ! SendMessage(message)

    case ReceiveMessage(message) =>
      Console.err.println(s"receiving message $message")
      addMessage(message)

    case m @ Disconnect =>
      jQ("#disconnect-button").text("Disconnecting ...").prop("disabled", true)
      proxyManager ! m

    case Disconnected =>
      service = context.system.deadLetters
      jQ("#connect-button").text("Connect").prop("disabled", false)
      jQ("#nickname-edit").prop("disabled", false)
      jQ("#send-button").prop("disabled", true)
      jQ("#status-disconnected").removeClass("status-hidden")
      jQ("#status-connected").addClass("status-hidden")
  }

  def addMessage(message: Message) = {
    jQ("#messages").append(jQ("<li>").text(
        s"${message.user.nick}: ${message.text}"))
  }
}

class ProxyManager extends Actor {
  def receive = {
    case ConnectAs(user) =>
      context.watch(context.actorOf(
          Props(new ClientProxy("ws://localhost:9000/chat-ws-entry"))))

    case m @ WebSocketConnected(entryPoint) =>
      context.parent.forward(m)

    case Disconnect =>
      context.children.foreach(context.stop(_))

    case Terminated(proxy) =>
      context.parent ! Disconnected
  }
}
