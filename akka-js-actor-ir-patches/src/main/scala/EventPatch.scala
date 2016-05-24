package akka.event

import akka.actor._

class DeadLetterListener {
  implicit var context: ActorContext = null
  implicit final var self: ActorRef = null
}

class DefaultLogger {
  implicit var context: ActorContext = null
  implicit final var self: ActorRef = null
}

class JSDefaultLogger {
  implicit var context: ActorContext = null
  implicit final var self: ActorRef = null
}

class EventStream {
  implicit var context: ActorContext = null
  implicit final var self: ActorRef = null
}

class EventStreamUnsubscriber {
  implicit var context: ActorContext = null
  implicit final var self: ActorRef = null
}

object Logging {
  class StandardOutLogger {
    implicit var context: ActorContext = null
    implicit final var self: ActorRef = null
  }
}
