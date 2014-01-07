package org.scalajs.actors

object Actor {
  /**
   * Type alias representing a Receive-expression for Akka Actors.
   */
  type Receive = PartialFunction[Any, Unit]

  /**
   * emptyBehavior is a Receive-expression that matches no messages at all, ever.
   */
  @SerialVersionUID(1L)
  object emptyBehavior extends Receive {
    def isDefinedAt(x: Any) = false
    def apply(x: Any) = throw new UnsupportedOperationException("Empty behavior apply()")
  }

  /**
   * Default placeholder (null) used for "!" to indicate that there is no sender of the message,
   * that will be translated to the receiving system's deadLetters.
   */
  final val noSender: ActorRef = null
}

trait Actor {

  import Actor._

  // to make type Receive known in subclasses without import
  type Receive = Actor.Receive

  /**
   * Stores the context for this actor, including self, and sender.
   * It is implicit to support operations such as `forward`.
   *
   * WARNING: Only valid within the Actor itself, so do not close over it and
   * publish it to other threads!
   *
   * [[akka.actor.ActorContext]] is the Scala API. `getContext` returns a
   * [[akka.actor.UntypedActorContext]], which is the Java API of the actor
   * context.
   */
  implicit val context: ActorContext = {
    val contextStack = ActorCell.contextStack
    if ((contextStack.isEmpty) || (contextStack.head eq null))
      throw ActorInitializationException(
        s"You cannot create an instance of [${getClass.getName}] explicitly using the constructor (new). " +
          "You have to use one of the 'actorOf' factory methods to create a new actor. See the documentation.")
    val c = contextStack.head
    ActorCell.contextStack = null :: contextStack
    c
  }

  /**
   * The 'self' field holds the ActorRef for this actor.
   * <p/>
   * Can be used to send messages to itself:
   * <pre>
   * self ! message
   * </pre>
   */
  implicit final val self = context.self //MUST BE A VAL, TRUST ME

  /**
   * The reference sender Actor of the last received message.
   * Is defined if the message was sent from another Actor,
   * else `deadLetters` in [[akka.actor.ActorSystem]].
   *
   * WARNING: Only valid within the Actor itself, so do not close over it and
   * publish it to other threads!
   */
  final def sender: ActorRef = context.sender

  /**
   * This defines the initial actor behavior, it must return a partial function
   * with the actor logic.
   */
  def receive: Actor.Receive

  /**
   * User overridable callback.
   * <p/>
   * Is called when a message isn't handled by the current behavior of the actor
   * by default it fails with either a [[akka.actor.DeathPactException]] (in
   * case of an unhandled [[akka.actor.Terminated]] message) or publishes an [[akka.actor.UnhandledMessage]]
   * to the actor's system's [[akka.event.EventStream]]
   */
  def unhandled(message: Any): Unit = {
    /*message match {
      case Terminated(dead) => throw new DeathPactException(dead)
      case _                => context.system.eventStream.publish(UnhandledMessage(message, sender, self))
    }*/
  }
}
