package ch.epfl.lamp.scalajs.actors

import scala.annotation.implicitNotFound

/**
 * Interface implemented by ActorSystem and ActorContext, the only two places
 * from which you can get fresh actors.
 */
@implicitNotFound("implicit ActorRefFactory required: if outside of an Actor you need an implicit ActorSystem, inside of an actor this should be the implicit ActorContext")
trait ActorRefFactory {
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
   * Stop the actor pointed to by the given [[akka.actor.ActorRef]]; this is
   * an asynchronous operation, i.e. involves a message send.
   */
  def stop(actor: ActorRef): Unit
}
