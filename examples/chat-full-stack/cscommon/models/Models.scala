package models

case class User(nick: String)

case class Message(user: User, text: String, timestamp: Long)
