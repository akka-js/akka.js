package org.scalajs.actors

/**
 * INTERNAL API
 *
 * Marker trait to show which Messages are automatically handled by Akka
 */
private[actors] trait AutoReceivedMessage

/**
 * Marker trait to indicate that a message might be potentially harmful,
 * this is used to block messages coming in over remoting.
 */
trait PossiblyHarmful

/**
 * A message all Actors will understand, that when processed will terminate
 * the Actor permanently.
 */
case object PoisonPill extends AutoReceivedMessage with PossiblyHarmful

/**
 * A message all Actors will understand, that when processed will make the
 * Actor throw an ActorKilledException, which will trigger supervision.
 */
case object Kill extends AutoReceivedMessage with PossiblyHarmful

/**
 * A message all Actors will understand, that when processed will reply with
 * [[org.scalajs.actors.ActorIdentity]] containing the `ActorRef`. The
 * `messageId` is returned in the `ActorIdentity` message as `correlationId`.
 */
final case class Identify(messageId: Any) extends AutoReceivedMessage

/**
 * Reply to [[org.scalajs.actors.Identify]]. Contains
 * `Some(ref)` with the `ActorRef` of the actor replying to the request or
 * `None` if no actor matched the request.
 * The `correlationId` is taken from the `messageId` in
 * the `Identify` message.
 */
final case class ActorIdentity(correlationId: Any, ref: Option[ActorRef])

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
final case class Terminated private[actors] (actor: ActorRef)(
    val existenceConfirmed: Boolean,
    val addressTerminated: Boolean) extends AutoReceivedMessage with PossiblyHarmful

/**
 * When using ActorContext.setReceiveTimeout, the singleton instance of
 * ReceiveTimeout will be sent to the Actor when there hasn't been any message
 * for that long.
 */
case object ReceiveTimeout extends PossiblyHarmful

/**
 * This message is published to the EventStream whenever an Actor receives a
 * message it doesn't understand.
 */
final case class UnhandledMessage(message: Any, sender: ActorRef,
    recipient: ActorRef)

/**
 * When a message is sent to an Actor that is terminated before receiving the
 * message, it will be sent as a DeadLetter to the ActorSystem's EventStream
 */
final case class DeadLetter(message: Any, sender: ActorRef,
    recipient: ActorRef) {
  require(sender ne null, "DeadLetter sender may not be null")
  require(recipient ne null, "DeadLetter recipient may not be null")
}
