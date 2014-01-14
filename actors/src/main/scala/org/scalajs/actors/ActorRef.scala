package org.scalajs.actors

import scala.language.implicitConversions

import sysmsg.SystemMessage

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

  /**
   * Forwards the message and passes the original sender actor as the sender.
   *
   * Works with '!' and '?'/'ask'.
   */
  def forward(message: Any)(implicit context: ActorContext): Unit =
    this.!(message)(context.sender)
}

/**
 * Internal trait for assembling all the functionality needed internally on
 * ActorRefs. NOTE THAT THIS IS NOT A STABLE EXTERNAL INTERFACE!
 */
private[actors] abstract class InternalActorRef extends ActorRef {
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

private[actors] object InternalActorRef {
  // TODO Not sure this is a good idea ...
  implicit def fromActorRef(ref: ActorRef): InternalActorRef =
    ref.asInstanceOf[InternalActorRef]
}

/**
 * Trait for ActorRef implementations where all methods contain default stubs.
 *
 * INTERNAL API
 */
private[actors] trait MinimalActorRef extends InternalActorRef {

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
private[actors] case object Nobody extends MinimalActorRef {
  override val path: RootActorPath =
    RootActorPath(Address("actors", "all-systems"), "/Nobody")
}
