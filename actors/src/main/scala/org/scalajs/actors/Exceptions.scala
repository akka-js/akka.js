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
