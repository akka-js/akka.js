package org.scalajs.actors

import scala.annotation.implicitNotFound

import scala.concurrent.ExecutionContext

/**
 * Interface implemented by ActorSystem and ActorContext, the only two places
 * from which you can get fresh actors.
 */
@implicitNotFound("implicit ActorRefFactory required: if outside of an Actor you need an implicit ActorSystem, inside of an actor this should be the implicit ActorContext")
trait ActorRefFactory {
  /**
   * INTERNAL API
   */
  protected def systemImpl: ActorSystemImpl

  /**
   * INTERNAL API
   */
  protected def provider: ActorRefProvider

  /**
   * Returns the default MessageDispatcher associated with this ActorRefFactory
   */
  implicit def dispatcher: ExecutionContext

  /**
   * Father of all children created by this interface.
   *
   * INTERNAL API
   */
  protected def guardian: InternalActorRef

  /**
   * INTERNAL API
   */
  protected def lookupRoot: InternalActorRef

  /**
   * Create new actor as child of this context and give it an automatically
   * generated name (currently similar to base64-encoded integer count,
   * reversed and with “$” prepended, may change in the future).
   *
   * See [[akka.actor.Props]] for details on how to obtain a `Props` object.
   *
   * @throws akka.ConfigurationException if deployment, dispatcher
   *   or mailbox configuration is wrong
   */
  def actorOf(props: Props): ActorRef

  /**
   * Create new actor as child of this context with the given name, which must
   * not be null, empty or start with “$”. If the given name is already in use,
   * and `InvalidActorNameException` is thrown.
   *
   * See [[akka.actor.Props]] for details on how to obtain a `Props` object.
   * @throws akka.actor.InvalidActorNameException if the given name is
   *   invalid or already in use
   * @throws akka.ConfigurationException if deployment, dispatcher
   *   or mailbox configuration is wrong
   */
  def actorOf(props: Props, name: String): ActorRef

  /**
   * Construct an [[akka.actor.ActorSelection]] from the given path, which is
   * parsed for wildcards (these are replaced by regular expressions
   * internally). No attempt is made to verify the existence of any part of
   * the supplied path, it is recommended to send a message and gather the
   * replies in order to resolve the matching set of actors.
   */
  /*def actorSelection(path: String): ActorSelection = path match {
    case RelativeActorPath(elems) =>
      if (elems.isEmpty) ActorSelection(provider.deadLetters, "")
      else if (elems.head.isEmpty) ActorSelection(provider.rootGuardian, elems.tail)
      else ActorSelection(lookupRoot, elems)
    case ActorPathExtractor(address, elems) =>
      ActorSelection(provider.rootGuardianAt(address), elems)
    case _ =>
      ActorSelection(provider.deadLetters, "")
  }*/

  /**
   * Construct an [[akka.actor.ActorSelection]] from the given path, which is
   * parsed for wildcards (these are replaced by regular expressions
   * internally). No attempt is made to verify the existence of any part of
   * the supplied path, it is recommended to send a message and gather the
   * replies in order to resolve the matching set of actors.
   */
  /*def actorSelection(path: ActorPath): ActorSelection =
    ActorSelection(provider.rootGuardianAt(path.address), path.elements)*/

  /**
   * Stop the actor pointed to by the given [[akka.actor.ActorRef]]; this is
   * an asynchronous operation, i.e. involves a message send.
   */
  def stop(actor: ActorRef): Unit
}
