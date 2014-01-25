package actors

import akka.actor._
import akka.actor.SupervisorStrategy.{Stop, Escalate}
import akka.event.LoggingReceive
import akka.scalajs.wsserver._

import models._

class ChatManager extends Actor with ActorLogging {
  RegisterPicklers.registerPicklers()

  val roomsManager = context.actorOf(Props[RoomsManager], name = "rooms")
  val usersManager = context.actorOf(
      Props(classOf[UsersManager], roomsManager), name = "users")

  roomsManager ! RoomsManager.SetUsersManager(usersManager)

  def receive = LoggingReceive {
    case m @ NewConnection() =>
      log.info("chat manager, new connection")
      usersManager.forward(m)
  }
}

object RoomsManager {
  case class SetUsersManager(ref: ActorRef)
  case class UserJoin(user: User, room: Room)
  case object AskRoomList
}
class RoomsManager extends Actor with ActorLogging {
  var usersManager: ActorRef = context.system.deadLetters
  var openRooms = Map.empty[ActorRef, Room]
  var openRoomsRev = Map.empty[Room, ActorRef]

  def receive = LoggingReceive {
    case RoomsManager.SetUsersManager(ref) =>
      usersManager = ref

    case m @ RoomsManager.UserJoin(user, room) =>
      val roomManager = openRoomsRev.getOrElse(room, {
        val man = context.watch(context.actorOf(
            Props(classOf[RoomManager], room)))
        openRooms += man -> room
        openRoomsRev += room -> man
        sendRoomListChanged()
        man
      })
      roomManager.forward(m)

    case RoomsManager.AskRoomList =>
      sender ! RoomListChanged(openRooms.values.toList)

    case Terminated(roomManager) =>
      openRooms.get(sender) foreach { room =>
        openRooms -= sender
        openRoomsRev -= room
        sendRoomListChanged()
      }
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _: Exception => Stop
      case t =>
        super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
    }

  def sendRoomListChanged(): Unit = {
    usersManager ! RoomListChanged(openRooms.values.toList)
  }
}

class RoomManager(val room: Room) extends Actor with ActorLogging {
  var attendingUsers = Map.empty[ActorRef, User]

  def receive = LoggingReceive {
    case RoomsManager.UserJoin(user, _) =>
      attendingUsers.keys foreach {
        _ ! UserJoined(user)
      }
      attendingUsers += sender -> user
      context.watch(sender)
      sender ! JoinedRoom(attendingUsers.values.toList)

    case Leave =>
      userLeave(sender)
      context.unwatch(sender)

    case SendMessage(message) =>
      // simulation of an exploit
      if (message.text == "exploit!kill-room")
        throw new Exception(s"room $room was killed by exploit!")

      sendToAllAttending(ReceiveMessage(message))

    case RequestPrivateChat(dest: User) =>
      attendingUsers.find(_._2 == dest).filter(_._1 != sender).fold {
        sender ! UserDoesNotExist
      } { case (ref, _) =>
        attendingUsers.get(sender) foreach { senderUser =>
          ref.forward(RequestPrivateChat(senderUser))
        }
      }

    case Terminated(ref) =>
      userLeave(ref)
  }

  def userLeave(ref: ActorRef): Unit = {
    attendingUsers.get(ref) foreach { user =>
      attendingUsers -= ref
      sendToAllAttending(UserLeft(user))

      if (attendingUsers.isEmpty)
        context.stop(self)
    }
  }

  def sendToAllAttending(msg: Any): Unit =
    attendingUsers.keys.foreach(_ ! msg)
}

class UsersManager(val roomsManager: ActorRef) extends Actor with ActorLogging {
  var connectedUsers = Map.empty[ActorRef, User]

  def receive = LoggingReceive {
    case m @ NewConnection() =>
      context.watch(context.actorOf(
          Props(classOf[UserManager], roomsManager))).forward(m)

    case m @ Connect(user) =>
      connectedUsers += sender -> user

    case m @ RoomListChanged(rooms) =>
      context.children.foreach(_ ! m)

    case Terminated(child) =>
      connectedUsers -= child
  }
}

class UserManager(val roomsManager: ActorRef) extends Actor with ActorLogging {
  var peer: ActorRef = context.system.deadLetters
  var user: User = User.Nobody

  def receive = LoggingReceive {
    case m @ NewConnection() =>
      sender ! ActorWebSocket.actorForWebSocketHandler(self)
      context.children.foreach(context.watch(_))

    case Connect(user) =>
      peer = sender
      this.user = user
      context.watch(peer)
      context.parent ! Connect(user)
      roomsManager ! RoomsManager.AskRoomList

    case m @ RoomListChanged(rooms) =>
      peer ! m

    case m @ Join(room: Room) =>
      roomsManager.forward(RoomsManager.UserJoin(user, room))

    case Terminated(peerOrProxy) =>
      context.stop(self)
  }
}
