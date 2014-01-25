package models

import akka.actor.ActorRef

// Actions from users
case class Connect(user: User)
case class Join(room: Room)
case object Leave
case class SendMessage(message: Message)

// Requests
case class RequestPrivateChat(peer: User, origin: ActorRef)
case object AcceptPrivateChat
case object RejectPrivateChat
case object UserDoesNotExist

// Notifications from server
case class RoomListChanged(rooms: List[Room])
case class JoinedRoom(users: List[User])
case class UserJoined(user: User)
case class UserLeft(user: User)
case class ReceiveMessage(message: Message)
