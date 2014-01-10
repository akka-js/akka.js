package org.scalajs.actors

class InvalidMessageException(msg: String) extends Exception(msg)

class ActorInitializationException(actor: ActorRef,
    msg: String, cause: Throwable) extends Exception(msg, cause) {
  def getActor(): ActorRef = actor
}

object ActorInitializationException {
  def apply(msg: String): ActorInitializationException =
    new ActorInitializationException(null, msg, null)

  def apply(actor: ActorRef, msg: String): ActorInitializationException =
    new ActorInitializationException(actor, msg, null)
}

/**
 * An InvalidActorNameException is thrown when you try to convert something,
 * usually a String, to an Actor name which doesn't validate.
 */
case class InvalidActorNameException(message: String) extends Exception(message)
