package akka.actor

import scala.collection.immutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

trait ActorContext extends ActorRefFactory {
  /**
   * Reference to this actor.
   */
  def self: ActorRef

  /**
   * Retrieve the Props which were used to create this actor.
   */
  def props: Props

  /**
   * Gets the current receive timeout.
   * When specified, the receive method should be able to handle a [[akka.actor.ReceiveTimeout]] message.
   */
  def receiveTimeout: Duration

  /**
   * Defines the inactivity timeout after which the sending of a [[akka.actor.ReceiveTimeout]] message is triggered.
   * When specified, the receive function should be able to handle a [[akka.actor.ReceiveTimeout]] message.
   * 1 millisecond is the minimum supported timeout.
   *
   * Please note that the receive timeout might fire and enqueue the `ReceiveTimeout` message right after
   * another message was enqueued; hence it is '''not guaranteed''' that upon reception of the receive
   * timeout there must have been an idle period beforehand as configured via this method.
   *
   * Once set, the receive timeout stays in effect (i.e. continues firing repeatedly after inactivity
   * periods). Pass in `Duration.Undefined` to switch off this feature.
   */
  def setReceiveTimeout(timeout: Duration): Unit

  /**
   * Changes the Actor's behavior to become the new 'Receive' (PartialFunction[Any, Unit]) handler.
   * This method acts upon the behavior stack as follows:
   *
   *  - if `discardOld = true` it will replace the top element (i.e. the current behavior)
   *  - if `discardOld = false` it will keep the current behavior and push the given one atop
   *
   * The default of replacing the current behavior has been chosen to avoid memory leaks in
   * case client code is written without consulting this documentation first (i.e. always pushing
   * new closures and never issuing an `unbecome()`)
   */
  def become(behavior: Actor.Receive, discardOld: Boolean = true): Unit

  /**
   * Reverts the Actor behavior to the previous one in the hotswap stack.
   */
  def unbecome(): Unit

  /**
   * Returns the sender 'ActorRef' of the current message.
   */
  def sender: ActorRef

  /**
   * Returns all supervised children; this method returns a view (i.e. a lazy
   * collection) onto the internal collection of children. Targeted lookups
   * should be using `child` instead for performance reasons:
   *
   * {{{
   * val badLookup = context.children find (_.path.name == "kid")
   * // should better be expressed as:
   * val goodLookup = context.child("kid")
   * }}}
   */
  def children: immutable.Iterable[ActorRef]

  /**
   * Get the child with the given name if it exists.
   */
  def child(name: String): Option[ActorRef]

  /**
   * Returns the dispatcher (MessageDispatcher) that is used for this Actor.
   * Importing this member will place an implicit ExecutionContext in scope.
   */
  implicit def dispatcher: ExecutionContext

  /**
   * The system that the actor belongs to.
   * Importing this member will place an implicit ExecutionContext in scope.
   */
  implicit def system: ActorSystem

  /**
   * Returns the supervising parent ActorRef.
   */
  def parent: ActorRef

  /**
   * Registers this actor as a Monitor for the provided ActorRef.
   * This actor will receive a Terminated(subject) message when watched
   * actor is terminated.
   * @return the provided ActorRef
   */
  def watch(subject: ActorRef): ActorRef

  /**
   * Unregisters this actor as Monitor for the provided ActorRef.
   * @return the provided ActorRef
   */
  def unwatch(subject: ActorRef): ActorRef
}
