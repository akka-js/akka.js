package akka.actor

import scala.annotation.unchecked.uncheckedStable

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

  private[this] var _context: ActorContext = {
    val contextStack = ActorCell.contextStack
    if ((contextStack.isEmpty) || (contextStack.head eq null))
      throw ActorInitializationException(
        s"You cannot create an instance of [${getClass.getName}] explicitly using the constructor (new). " +
          "You have to use one of the 'actorOf' factory methods to create a new actor. See the documentation.")
    val c = contextStack.head
    ActorCell.contextStack = null :: contextStack
    c
  }

  private[this] var _self: ActorRef = context.self

  private[akka] final def setActorFields(context: ActorContext,
      self: ActorRef): Unit = {
    this._context = context
    this._self = self
  }

  /**
   * Stores the context for this actor, including self, and sender.
   * It is implicit to support operations such as `forward`.
   *
   * WARNING: Only valid within the Actor itself, so do not close over it and
   * publish it to other threads!
   */
  implicit final def context: ActorContext = _context

  /**
   * The 'self' field holds the ActorRef for this actor.
   * <p/>
   * Can be used to send messages to itself:
   * <pre>
   * self ! message
   * </pre>
   */
  implicit final def self: ActorRef = _self

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
   * User overridable definition the strategy to use for supervising
   * child actors.
   */
  def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.defaultStrategy

  // Life-cycle hooks

  /**
   * User overridable callback.
   * <p/>
   * Is called when an Actor is started.
   * Actors are automatically started asynchronously when created.
   * Empty default implementation.
   */
  def preStart(): Unit = ()

  /**
   * User overridable callback.
   * <p/>
   * Is called asynchronously after 'actor.stop()' is invoked.
   * Empty default implementation.
   */
  def postStop(): Unit = ()

  /**
   * User overridable callback: '''By default it disposes of all children and
   * then calls `postStop()`.'''
   * @param reason the Throwable that caused the restart to happen
   * @param message optionally the current message the actor processed when failing, if applicable
   * <p/>
   * Is called on a crashed Actor right BEFORE it is restarted to allow clean
   * up of resources before Actor is terminated.
   */
  def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    context.children foreach { child =>
      context.unwatch(child)
      context.stop(child)
    }
    postStop()
  }

  /**
   * User overridable callback: By default it calls `preStart()`.
   * @param reason the Throwable that caused the restart to happen
   * <p/>
   * Is called right AFTER restart on the newly created Actor to allow
   * reinitialization after an Actor crash.
   */
  def postRestart(reason: Throwable): Unit = {
    preStart()
  }

  // Unhandled message

  /**
   * User overridable callback.
   * <p/>
   * Is called when a message isn't handled by the current behavior of the actor
   * by default it fails with either a [[akka.actor.DeathPactException]] (in
   * case of an unhandled [[akka.actor.Terminated]] message) or publishes an [[akka.actor.UnhandledMessage]]
   * to the actor's system's [[akka.event.EventStream]]
   */
  def unhandled(message: Any): Unit = {
    message match {
      case Terminated(dead) => throw new DeathPactException(dead)
      case _                =>
        context.system.eventStream.publish(UnhandledMessage(message, sender, self))
    }
  }
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
trait ActorLogging { this: Actor =>
  //val log = akka.event.Logging(context.system, this)
  object log {
    import akka.event.Logging._
    private def publish(event: LogEvent) = context.system.eventStream.publish(event)
    private def myClass = ActorLogging.this.getClass

    def debug(msg: String): Unit = publish(Debug(self.toString, myClass, msg))
    def info(msg: String): Unit = publish(Info(self.toString, myClass, msg))
    def warning(msg: String): Unit = publish(Warning(self.toString, myClass, msg))
    def error(msg: String): Unit = publish(Error(self.toString, myClass, msg))
  }
}
