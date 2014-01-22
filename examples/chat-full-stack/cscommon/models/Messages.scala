package models

case class Join(user: User)

case class SendMessage(message: Message)

case class ReceiveMessage(message: Message)
