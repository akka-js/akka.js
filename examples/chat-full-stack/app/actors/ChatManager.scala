package actors

import akka.actor._
import akka.event.LoggingReceive
import akka.scalajs.wsserver._

import models._

class ChatManager extends Actor with ActorLogging {
  RegisterPicklers.registerPicklers()

  val usersManager = context.actorOf(Props[UsersManager], name = "users")

  def receive = LoggingReceive {
    case m @ NewConnection() =>
      log.info("chat manager, new connection")
      usersManager.forward(m)
  }
}

class UsersManager extends Actor with ActorLogging {
  def receive = LoggingReceive {
    case m @ NewConnection() =>
      context.actorOf(Props[UserManager]).forward(m)

    case m @ SendMessage(message) =>
      log.info(s"manager forwards $m to all its children")
      context.children.foreach(_ ! ReceiveMessage(message))
  }
}

class UserManager extends Actor with ActorLogging {
  var peer: ActorRef = context.system.deadLetters
  var user: User = User("<no-one>")

  def receive = LoggingReceive {
    case m @ NewConnection() =>
      sender ! ActorWebSocket.actorForWebSocketHandler(self)

    case Join(user) =>
      peer = sender
      this.user = user

    case m @ SendMessage(message) =>
      log.info(s"user $user sends $m")
      context.parent ! m

    case m @ ReceiveMessage(message) =>
      log.info(s"user $user receives $m, peer is $peer")
      peer ! m

    case m =>
      log.info(s"hey look! I receive this unhandled message: $m")
  }
}
