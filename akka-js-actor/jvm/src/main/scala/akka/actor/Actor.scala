/**
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.actor

object Actor {
  /**
   * Type alias representing a Receive-expression for Akka Actors.
   */
  //#receive
  type Receive = PartialFunction[Any, Unit]

  //#receive

  /**
   * emptyBehavior is a Receive-expression that matches no messages at all, ever.
   */
  @SerialVersionUID(1L)
  object emptyBehavior extends Receive {
    def isDefinedAt(x: Any) = false
    def apply(x: Any) = throw new UnsupportedOperationException("Empty behavior apply()")
  }

  /**
   * ignoringBehavior is a Receive-expression that consumes and ignores all messages.
   */
  @SerialVersionUID(1L)
  object ignoringBehavior extends Receive {
    def isDefinedAt(x: Any): Boolean = true
    def apply(x: Any): Unit = ()
  }

  /**
   * Default placeholder (null) used for "!" to indicate that there is no sender of the message,
   * that will be translated to the receiving system's deadLetters.
   */
  final val noSender: ActorRef = null
}

/**
 * Actor base trait that should be extended by or mixed to create an Actor with the semantics of the 'Actor Model':
 * <a href="http://en.wikipedia.org/wiki/Actor_model">http://en.wikipedia.org/wiki/Actor_model</a>
 *
 * An actor has a well-defined (non-cyclic) life-cycle.
 *  - ''RUNNING'' (created and started actor) - can receive messages
 *  - ''SHUTDOWN'' (when 'stop' is invoked) - can't do anything
 *
 * The Actor's own [[akka.actor.ActorRef]] is available as `self`, the current
 * message’s sender as `sender()` and the [[akka.actor.ActorContext]] as
 * `context`. The only abstract method is `receive` which shall return the
 * initial behavior of the actor as a partial function (behavior can be changed
 * using `context.become` and `context.unbecome`).
 *
 * This is the Scala API (hence the Scala code below), for the Java API see [[akka.actor.UntypedActor]].
 *
 * {{{
 * class ExampleActor extends Actor {
 *
 *   override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
 *     case _: ArithmeticException      => Resume
 *     case _: NullPointerException     => Restart
 *     case _: IllegalArgumentException => Stop
 *     case _: Exception                => Escalate
 *   }
 *
 *   def receive = {
 *                                      // directly calculated reply
 *     case Request(r)               => sender() ! calculate(r)
 *
 *                                      // just to demonstrate how to stop yourself
 *     case Shutdown                 => context.stop(self)
 *
 *                                      // error kernel with child replying directly to 'sender()'
 *     case Dangerous(r)             => context.actorOf(Props[ReplyToOriginWorker]).tell(PerformWork(r), sender())
 *
 *                                      // error kernel with reply going through us
 *     case OtherJob(r)              => context.actorOf(Props[ReplyToMeWorker]) ! JobRequest(r, sender())
 *     case JobReply(result, orig_s) => orig_s ! result
 *   }
 * }
 * }}}
 *
 * The last line demonstrates the essence of the error kernel design: spawn
 * one-off actors which terminate after doing their job, pass on `sender()` to
 * allow direct reply if that is what makes sense, or round-trip the sender
 * as shown with the fictitious JobRequest/JobReply message pair.
 *
 * If you don’t like writing `context` you can always `import context._` to get
 * direct access to `actorOf`, `stop` etc. This is not default in order to keep
 * the name-space clean.
 */
trait Actor {

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
    val contextStack = ActorCell.contextStack.get
    if ((contextStack.isEmpty) || (contextStack.head eq null))
      throw ActorInitializationException(
        s"You cannot create an instance of [${getClass.getName}] explicitly using the constructor (new). " +
          "You have to use one of the 'actorOf' factory methods to create a new actor. See the documentation.")
    val c = contextStack.head
    ActorCell.contextStack.set(null :: contextStack)
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
  final def sender(): ActorRef = context.sender()

  /**
   * This defines the initial actor behavior, it must return a partial function
   * with the actor logic.
   */
  //#receive
  def receive: Actor.Receive
  //#receive

  /**
   * INTERNAL API.
   *
   * Can be overridden to intercept calls to this actor's current behavior.
   *
   * @param receive current behavior.
   * @param msg current message.
   */
  protected[akka] def aroundReceive(receive: Actor.Receive, msg: Any): Unit = receive.applyOrElse(msg, unhandled)

  /**
   * Can be overridden to intercept calls to `preStart`. Calls `preStart` by default.
   */
  protected[akka] def aroundPreStart(): Unit = preStart()

  /**
   * Can be overridden to intercept calls to `postStop`. Calls `postStop` by default.
   */
  protected[akka] def aroundPostStop(): Unit = postStop()

  /**
   * Can be overridden to intercept calls to `preRestart`. Calls `preRestart` by default.
   */
  protected[akka] def aroundPreRestart(reason: Throwable, message: Option[Any]): Unit = preRestart(reason, message)

  /**
   * Can be overridden to intercept calls to `postRestart`. Calls `postRestart` by default.
   */
  protected[akka] def aroundPostRestart(reason: Throwable): Unit = postRestart(reason)

  /**
   * User overridable definition the strategy to use for supervising
   * child actors.
   */
  def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.defaultStrategy

  /**
   * User overridable callback.
   * <p/>
   * Is called when an Actor is started.
   * Actors are automatically started asynchronously when created.
   * Empty default implementation.
   */
  @throws(classOf[Exception]) // when changing this you MUST also change UntypedActorDocTest
  //#lifecycle-hooks
  def preStart(): Unit = ()

  //#lifecycle-hooks

  /**
   * User overridable callback.
   * <p/>
   * Is called asynchronously after 'actor.stop()' is invoked.
   * Empty default implementation.
   */
  @throws(classOf[Exception]) // when changing this you MUST also change UntypedActorDocTest
  //#lifecycle-hooks
  def postStop(): Unit = ()

  //#lifecycle-hooks

  /**
   * User overridable callback: '''By default it disposes of all children and then calls `postStop()`.'''
   * @param reason the Throwable that caused the restart to happen
   * @param message optionally the current message the actor processed when failing, if applicable
   * <p/>
   * Is called on a crashed Actor right BEFORE it is restarted to allow clean
   * up of resources before Actor is terminated.
   */
  @throws(classOf[Exception]) // when changing this you MUST also change UntypedActorDocTest
  //#lifecycle-hooks
  def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    context.children foreach { child ⇒
      context.unwatch(child)
      context.stop(child)
    }
    postStop()
  }

  //#lifecycle-hooks

  /**
   * User overridable callback: By default it calls `preStart()`.
   * @param reason the Throwable that caused the restart to happen
   * <p/>
   * Is called right AFTER restart on the newly created Actor to allow reinitialization after an Actor crash.
   */
  @throws(classOf[Exception]) // when changing this you MUST also change UntypedActorDocTest
  //#lifecycle-hooks
  def postRestart(reason: Throwable): Unit = {
    preStart()
  }
  //#lifecycle-hooks

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
      case Terminated(dead) ⇒ throw new DeathPactException(dead)
      case _                ⇒ context.system.eventStream.publish(UnhandledMessage(message, sender(), self))
    }
  }
}
