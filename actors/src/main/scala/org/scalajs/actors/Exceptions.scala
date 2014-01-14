package org.scalajs.actors

import scala.annotation.tailrec

/** Base class for actors exceptions. */
class ActorsException(message: String,
    cause: Throwable) extends Exception(message, cause) {
  def this(message: String) = this(message, null)
}

/**
 * IllegalActorStateException is thrown when a core invariant in the Actor
 * implementation has been violated.
 * For instance, if you try to create an Actor that doesn't extend Actor.
 */
final case class IllegalActorStateException private[actors] (
    message: String) extends ActorsException(message)

/**
 * ActorKilledException is thrown when an Actor receives the
 * [[org.scalajs.actors.Kill]] message
 */
final case class ActorKilledException private[actors] (
    message: String) extends ActorsException(message)

/**
 * An InvalidActorNameException is thrown when you try to convert something,
 * usually a String, to an Actor name which doesn't validate.
 */
final case class InvalidActorNameException(
    message: String) extends ActorsException(message)

/**
 * An ActorInitializationException is thrown when the the initialization logic
 * for an Actor fails.
 *
 * There is an extractor which works for ActorInitializationException and its
 * subtypes:
 *
 * {{{
 * ex match {
 *   case ActorInitializationException(actor, message, cause) => ...
 * }
 * }}}
 */
class ActorInitializationException protected (actor: ActorRef,
    message: String, cause: Throwable)
    extends ActorsException(message, cause) {
  def getActor(): ActorRef = actor
}

object ActorInitializationException {
  private[actors] def apply(actor: ActorRef, message: String, cause: Throwable = null): ActorInitializationException =
    new ActorInitializationException(actor, message, cause)
  private[actors] def apply(message: String): ActorInitializationException =
    new ActorInitializationException(null, message, null)
  def unapply(ex: ActorInitializationException): Option[(ActorRef, String, Throwable)] =
    Some((ex.getActor, ex.getMessage, ex.getCause))
}

/**
 * A PreRestartException is thrown when the preRestart() method failed; this
 * exception is not propagated to the supervisor, as it originates from the
 * already failed instance, hence it is only visible as log entry on the event
 * stream.
 *
 * @param actor is the actor whose preRestart() hook failed
 * @param cause is the exception thrown by that actor within preRestart()
 * @param originalCause is the exception which caused the restart in the first place
 * @param messageOption is the message which was optionally passed into preRestart()
 */
final case class PreRestartException private[actors] (actor: ActorRef,
    cause: Throwable, originalCause: Throwable, messageOption: Option[Any])
    extends ActorInitializationException(actor,
        "exception in preRestart(" +
        (if (originalCause == null) "null" else originalCause.getClass) + ", " +
        (messageOption match { case Some(m: AnyRef) => m.getClass; case _ => "None" }) +
        ")", cause)

/**
 * A PostRestartException is thrown when constructor or postRestart() method
 * fails during a restart attempt.
 *
 * @param actor is the actor whose constructor or postRestart() hook failed
 * @param cause is the exception thrown by that actor within preRestart()
 * @param originalCause is the exception which caused the restart in the first place
 */
final case class PostRestartException private[actors] (actor: ActorRef,
    cause: Throwable, originalCause: Throwable)
    extends ActorInitializationException(actor,
        "exception post restart (" + (
            if (originalCause == null) "null"
            else originalCause.getClass) + ")", cause)

/**
 * This is an extractor for retrieving the original cause (i.e. the first
 * failure) from a [[org.scalajs.actors.PostRestartException]]. In the face of
 * multiple “nested” restarts it will walk the origCause-links until it arrives
 * at a non-PostRestartException type.
 */
object OriginalRestartException {
  def unapply(ex: PostRestartException): Option[Throwable] = {
    @tailrec def rec(ex: PostRestartException): Option[Throwable] = ex match {
      case PostRestartException(_, _, e: PostRestartException) => rec(e)
      case PostRestartException(_, _, e)                       => Some(e)
    }
    rec(ex)
  }
}

/**
 * InvalidMessageException is thrown when an invalid message is sent to an
 * Actor.
 * Currently only `null` is an invalid message.
 */
final case class InvalidMessageException private[actors] (
    message: String) extends ActorsException(message)

/**
 * A DeathPactException is thrown by an Actor that receives a
 * Terminated(someActor) message that it doesn't handle itself, effectively
 * crashing the Actor and escalating to the supervisor.
 */
final case class DeathPactException private[actors] (dead: ActorRef)
    extends ActorsException("Monitored actor [" + dead + "] terminated")
