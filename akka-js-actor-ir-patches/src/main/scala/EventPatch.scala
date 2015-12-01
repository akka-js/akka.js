package akka.event

import akka.actor._

//probably this will be removed at a certain point
class LoggingBusActor {
  implicit var context: ActorContext = null
  implicit final var self: ActorRef = null
}

class DeadLetterListener {
  implicit var context: ActorContext = null
  implicit final var self: ActorRef = null
}

class DefaultLogger {
  implicit var context: ActorContext = null
  implicit final var self: ActorRef = null
}
