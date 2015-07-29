/**
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.actor

import akka.AkkaException
import akka.event.LoggingAdapter

import scala.annotation.tailrec
import scala.beans.BeanProperty
import scala.util.control.NoStackTrace

/**
 * INTERNAL API
 *
 * Marker trait to show which Messages are automatically handled by Akka
 */
private[akka] trait AutoReceivedMessage extends Serializable

/**
 * Marker trait to indicate that a message might be potentially harmful,
 * this is used to block messages coming in over remoting.
 */
trait PossiblyHarmful

/**
 * Marker trait to signal that this class should not be verified for serializability.
 */
trait NoSerializationVerificationNeeded

abstract class PoisonPill extends AutoReceivedMessage with PossiblyHarmful with DeadLetterSuppression

/**
 * A message all Actors will understand, that when processed will terminate the Actor permanently.
 */
@SerialVersionUID(1L)
case object PoisonPill extends PoisonPill {
  /**
   * Java API: get the singleton instance
   */
  def getInstance = this
}

abstract class Kill extends AutoReceivedMessage with PossiblyHarmful
/**
 * A message all Actors will understand, that when processed will make the Actor throw an ActorKilledException,
 * which will trigger supervision.
 */
@SerialVersionUID(1L)
case object Kill extends Kill {
  /**
   * Java API: get the singleton instance
   */
  def getInstance = this
}

/**
 * A message all Actors will understand, that when processed will reply with
 * [[akka.actor.ActorIdentity]] containing the `ActorRef`. The `messageId`
 * is returned in the `ActorIdentity` message as `correlationId`.
 */
@SerialVersionUID(1L)
final case class Identify(messageId: Any) extends AutoReceivedMessage

/**
 * Reply to [[akka.actor.Identify]]. Contains
 * `Some(ref)` with the `ActorRef` of the actor replying to the request or
 * `None` if no actor matched the request.
 * The `correlationId` is taken from the `messageId` in
 * the `Identify` message.
 */
@SerialVersionUID(1L)
final case class ActorIdentity(correlationId: Any, ref: Option[ActorRef]) {
  /**
   * Java API: `ActorRef` of the actor replying to the request or
   * null if no actor matched the request.
   */
  def getRef: ActorRef = ref.orNull
}

/**
 * When Death Watch is used, the watcher will receive a Terminated(watched)
 * message when watched is terminated.
 * Terminated message can't be forwarded to another actor, since that actor
 * might not be watching the subject. Instead, if you need to forward Terminated
 * to another actor you should send the information in your own message.
 *
 * @param actor the watched actor that terminated
 * @param existenceConfirmed is false when the Terminated message was not sent
 *   directly from the watched actor, but derived from another source, such as
 *   when watching a non-local ActorRef, which might not have been resolved
 * @param addressTerminated the Terminated message was derived from
 *   that the remote node hosting the watched actor was detected as unreachable
 */
@SerialVersionUID(1L)
final case class Terminated private[akka] (@BeanProperty actor: ActorRef)(
  @BeanProperty val existenceConfirmed: Boolean,
  @BeanProperty val addressTerminated: Boolean)
  extends AutoReceivedMessage with PossiblyHarmful with DeadLetterSuppression

/**
 * INTERNAL API
 *
 * Used for remote death watch. Failure detector publish this to the
 * [[akka.event.AddressTerminatedTopic]] when a remote node is detected to be unreachable and/or decided to
 * be removed.
 * The watcher ([[akka.actor.dungeon.DeathWatch]]) subscribes to the `AddressTerminatedTopic`
 * and translates this event to [[akka.actor.Terminated]], which is sent itself.
 */
@SerialVersionUID(1L)
private[akka] final case class AddressTerminated(address: Address)
  extends AutoReceivedMessage with PossiblyHarmful with DeadLetterSuppression

abstract class ReceiveTimeout extends PossiblyHarmful

/**
 * When using ActorContext.setReceiveTimeout, the singleton instance of ReceiveTimeout will be sent
 * to the Actor when there hasn't been any message for that long.
 */
@SerialVersionUID(1L)
case object ReceiveTimeout extends ReceiveTimeout {
  /**
   * Java API: get the singleton instance
   */
  def getInstance = this
}

/**
 * IllegalActorStateException is thrown when a core invariant in the Actor implementation has been violated.
 * For instance, if you try to create an Actor that doesn't extend Actor.
 */
@SerialVersionUID(1L)
final case class IllegalActorStateException private[akka] (message: String) extends AkkaException(message)

/**
 * ActorKilledException is thrown when an Actor receives the [[akka.actor.Kill]] message
 */
@SerialVersionUID(1L)
final case class ActorKilledException private[akka] (message: String) extends AkkaException(message) with NoStackTrace

/**
 * An InvalidActorNameException is thrown when you try to convert something, usually a String, to an Actor name
 * which doesn't validate.
 */
@SerialVersionUID(1L)
final case class InvalidActorNameException(message: String) extends AkkaException(message)

/**
 * An ActorInitializationException is thrown when the initialization logic for an Actor fails.
 *
 * There is an extractor which works for ActorInitializationException and its subtypes:
 *
 * {{{
 * ex match {
 *   case ActorInitializationException(actor, message, cause) => ...
 * }
 * }}}
 */
@SerialVersionUID(1L)
class ActorInitializationException protected (actor: ActorRef, message: String, cause: Throwable)
  extends AkkaException(message, cause) {
  def getActor: ActorRef = actor
}
object ActorInitializationException {
  private[akka] def apply(actor: ActorRef, message: String, cause: Throwable = null): ActorInitializationException =
    new ActorInitializationException(actor, message, cause)
  private[akka] def apply(message: String): ActorInitializationException = new ActorInitializationException(null, message, null)
  def unapply(ex: ActorInitializationException): Option[(ActorRef, String, Throwable)] = Some((ex.getActor, ex.getMessage, ex.getCause))
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
@SerialVersionUID(1L)
final case class PreRestartException private[akka] (actor: ActorRef, cause: Throwable, originalCause: Throwable, messageOption: Option[Any])
  extends ActorInitializationException(actor,
    "exception in preRestart(" +
      (if (originalCause == null) "null" else originalCause.getClass) + ", " +
      (messageOption match { case Some(m: AnyRef) ⇒ m.getClass; case _ ⇒ "None" }) +
      ")", cause)

/**
 * A PostRestartException is thrown when constructor or postRestart() method
 * fails during a restart attempt.
 *
 * @param actor is the actor whose constructor or postRestart() hook failed
 * @param cause is the exception thrown by that actor within preRestart()
 * @param originalCause is the exception which caused the restart in the first place
 */
@SerialVersionUID(1L)
final case class PostRestartException private[akka] (actor: ActorRef, cause: Throwable, originalCause: Throwable)
  extends ActorInitializationException(actor,
    "exception post restart (" + (if (originalCause == null) "null" else originalCause.getClass) + ")", cause)

/**
 * This is an extractor for retrieving the original cause (i.e. the first
 * failure) from a [[akka.actor.PostRestartException]]. In the face of multiple
 * “nested” restarts it will walk the origCause-links until it arrives at a
 * non-PostRestartException type.
 */
@SerialVersionUID(1L)
object OriginalRestartException {
  def unapply(ex: PostRestartException): Option[Throwable] = {
    @tailrec def rec(ex: PostRestartException): Option[Throwable] = ex match {
      case PostRestartException(_, _, e: PostRestartException) ⇒ rec(e)
      case PostRestartException(_, _, e)                       ⇒ Some(e)
    }
    rec(ex)
  }
}

/**
 * InvalidMessageException is thrown when an invalid message is sent to an Actor;
 * Currently only `null` is an invalid message.
 */
@SerialVersionUID(1L)
final case class InvalidMessageException private[akka] (message: String) extends AkkaException(message)

/**
 * A DeathPactException is thrown by an Actor that receives a Terminated(someActor) message
 * that it doesn't handle itself, effectively crashing the Actor and escalating to the supervisor.
 */
@SerialVersionUID(1L)
final case class DeathPactException private[akka] (dead: ActorRef)
  extends AkkaException("Monitored actor [" + dead + "] terminated")
  with NoStackTrace

/**
 * When an InterruptedException is thrown inside an Actor, it is wrapped as an ActorInterruptedException as to
 * avoid cascading interrupts to other threads than the originally interrupted one.
 */
@SerialVersionUID(1L)
class ActorInterruptedException private[akka] (cause: Throwable) extends AkkaException(cause.getMessage, cause)

/**
 * This message is published to the EventStream whenever an Actor receives a message it doesn't understand
 */
@SerialVersionUID(1L)
final case class UnhandledMessage(@BeanProperty message: Any, @BeanProperty sender: ActorRef, @BeanProperty recipient: ActorRef)

/**
 * Classes for passing status back to the sender.
 * Used for internal ACKing protocol. But exposed as utility class for user-specific ACKing protocols as well.
 */
object Status {
  sealed trait Status extends Serializable

  /**
   * This class/message type is preferably used to indicate success of some operation performed.
   */
  @SerialVersionUID(1L)
  final case class Success(status: Any) extends Status

  /**
   * This class/message type is preferably used to indicate failure of some operation performed.
   * As an example, it is used to signal failure with AskSupport is used (ask/?).
   */
  @SerialVersionUID(1L)
  final case class Failure(cause: Throwable) extends Status
}

/**
 * Scala API: Mix in ActorLogging into your Actor to easily obtain a reference to a logger,
 * which is available under the name "log".
 *
 * {{{
 * class MyActor extends Actor with ActorLogging {
 *   def receive = {
 *     case "pigdog" => log.info("We've got yet another pigdog on our hands")
 *   }
 * }
 * }}}
 */
trait ActorLogging { this: Actor ⇒
  private var _log: LoggingAdapter = _

  def log: LoggingAdapter = {
    // only used in Actor, i.e. thread safe
    if (_log eq null)
      _log = akka.event.Logging(context.system, this)
    _log
  }

}

/**
 * Scala API: Mix in DiagnosticActorLogging into your Actor to easily obtain a reference to a logger with MDC support,
 * which is available under the name "log".
 * In the example bellow "the one who knocks" will be available under the key "iam" for using it in the logback pattern.
 *
 * {{{
 * class MyActor extends Actor with DiagnosticActorLogging {
 *
 *   override def mdc(currentMessage: Any): MDC = {
 *     Map("iam", "the one who knocks")
 *   }
 *
 *   def receive = {
 *     case "pigdog" => log.info("We've got yet another pigdog on our hands")
 *   }
 * }
 * }}}
 */
trait DiagnosticActorLogging extends Actor {
  import akka.event.Logging._
  val log = akka.event.Logging(this)
  def mdc(currentMessage: Any): MDC = emptyMDC

  override protected[akka] def aroundReceive(receive: Actor.Receive, msg: Any): Unit = try {
    log.mdc(mdc(msg))
    super.aroundReceive(receive, msg)
  } finally {
    log.clearMDC()
  }
}
