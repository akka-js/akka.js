package akka.actor

import scala.language.implicitConversions

import akka.dispatch.sysmsg.SystemMessage

/**
 * Immutable and serializable handle to an actor, which may or may not reside
 * on the local host or inside the same [[org.scalajs.actors.ActorSystem]]. An ActorRef
 * can be obtained from an [[org.scalajs.actors.ActorRefFactory]], an interface which
 * is implemented by ActorSystem and [[org.scalajs.actors.ActorContext]]. This means
 * actors can be created top-level in the ActorSystem or as children of an
 * existing actor, but only from within that actor.
 *
 * ActorRefs can be freely shared among actors by message passing. Message
 * passing conversely is their only purpose.
 *
 * ActorRef does not have a method for terminating the actor it points to, use
 * [[akka.actor.ActorRefFactory]]`.stop(ref)`, or send a [[akka.actor.PoisonPill]],
 * for this purpose.
 *
 * Two actor references are compared equal when they have the same path and point to
 * the same actor incarnation. A reference pointing to a terminated actor doesn't compare
 * equal to a reference pointing to another (re-created) actor with the same path.
 * Actor references acquired with `actorFor` do not always include the full information
 * about the underlying actor identity and therefore such references do not always compare
 * equal to references acquired with `actorOf`, `sender`, or `context.self`.
 *
 * If you need to keep track of actor references in a collection and do not care
 * about the exact actor incarnation you can use the ``ActorPath`` as key because
 * the unique id of the actor is not taken into account when comparing actor paths.
 */
abstract class ActorRef { internalRef: InternalActorRef =>

  def path: ActorPath

  /**
   * Sends a one-way asynchronous message. E.g. fire-and-forget semantics.
   * <p/>
   *
   * If invoked from within an actor then the actor reference is implicitly passed on as the implicit 'sender' argument.
   * <p/>
   *
   * This actor 'sender' reference is then available in the receiving actor in the 'sender' member variable,
   * if invoked from within an Actor. If not then no sender is available.
   * <pre>
   *   actor ! message
   * </pre>
   * <p/>
   */
  def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit

  def tell(message: Any, sender: ActorRef): Unit = this.!(message)(sender)

  /**
   * Forwards the message and passes the original sender actor as the sender.
   *
   * Works with '!' and '?'/'ask'.
   */
  def forward(message: Any)(implicit context: ActorContext): Unit =
    this.!(message)(context.sender)

  /**
   * Comparison takes path and the unique id of the actor cell into account.
   */
  final def compareTo(other: ActorRef) = {
    val x = this.path compareTo other.path
    if (x == 0) {
      if (this.path.uid < other.path.uid) -1
      else if (this.path.uid == other.path.uid) 0
      else 1
    }
    else x
  }

  final override def hashCode: Int = {
    if (path.uid == ActorCell.undefinedUid) path.hashCode
    else path.uid
  }

  /**
   * Equals takes path and the unique id of the actor cell into account.
   */
  final override def equals(that: Any): Boolean = that match {
    case other: ActorRef => path.uid == other.path.uid && path == other.path
    case _               => false
  }

  override def toString: String =
    if (path.uid == ActorCell.undefinedUid) s"Actor[${path}]"
    else s"Actor[${path}#${path.uid}]"
}

/**
 * Internal trait for assembling all the functionality needed internally on
 * ActorRefs. NOTE THAT THIS IS NOT A STABLE EXTERNAL INTERFACE!
 */
private[akka] abstract class InternalActorRef extends ActorRef {
  /*
   * Actor life-cycle management, invoked only internally (in response to user requests via ActorContext).
   */
  def start(): Unit
  def resume(causedByFailure: Throwable): Unit
  def suspend(): Unit
  def restart(cause: Throwable): Unit
  def stop(): Unit
  def sendSystemMessage(message: SystemMessage): Unit

  /**
   * Get a reference to the actor ref provider which created this ref.
   */
  def provider: ActorRefProvider

  /**
   * Obtain parent of this ref; used by getChild for ".." paths.
   */
  def getParent: InternalActorRef

  /**
   * Obtain ActorRef by possibly traversing the actor tree or looking it up at
   * some provider-specific location. This method shall return the end result,
   * i.e. not only the next step in the look-up; this will typically involve
   * recursive invocation. A path element of ".." signifies the parent, a
   * trailing "" element must be disregarded. If the requested path does not
   * exist, return Nobody.
   */
  def getChild(name: Iterator[String]): InternalActorRef

  /**
   * Scope: if this ref points to an actor which resides within the same
   * JS VM, i.e., whose mailbox is directly reachable etc.
   */
  def isLocal: Boolean
}

private[akka] object InternalActorRef {
  // TODO Not sure this is a good idea ...
  implicit def fromActorRef(ref: ActorRef): InternalActorRef =
    ref.asInstanceOf[InternalActorRef]
}

/**
 * Trait for ActorRef implementations where all methods contain default stubs.
 *
 * INTERNAL API
 */
private[akka] trait MinimalActorRef extends InternalActorRef {

  override def start(): Unit = ()
  override def suspend(): Unit = ()
  override def resume(causedByFailure: Throwable): Unit = ()
  override def stop(): Unit = ()
  override def restart(cause: Throwable): Unit = ()

  override def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit = ()

  override def sendSystemMessage(message: SystemMessage): Unit = ()

  override def getParent: InternalActorRef = Nobody
  override def getChild(names: Iterator[String]): InternalActorRef =
    if (names.forall(_.isEmpty)) this else Nobody

  override def isLocal: Boolean = true
}

/**
 * This is an internal look-up failure token, not useful for anything else.
 */
private[akka] case object Nobody extends MinimalActorRef {
  override val path: RootActorPath =
    RootActorPath(Address("actors", "all-systems"), "/Nobody")

  override def provider =
    throw new UnsupportedOperationException("Nobody does not provide")
}
