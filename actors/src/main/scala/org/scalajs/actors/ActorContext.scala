package org.scalajs.actors

import scala.util.control.NonFatal

import dispatch.MessageDispatcher
import sysmsg._

trait ActorContext {
  /**
   * Reference to this actor.
   */
  def self: ActorRef

  /**
   * Retrieve the Props which were used to create this actor.
   */
  def props: Props

  /**
   * Returns the sender 'ActorRef' of the current message.
   */
  def sender: ActorRef

  /**
   * The system that the actor belongs to.
   * Importing this member will place a implicit ExecutionContext in scope.
   */
  implicit def system: ActorSystem
}

private[actors] object ActorCell {
  var contextStack: List[ActorContext] = Nil

  final val emptyBehaviorStack: List[Actor.Receive] = Nil
}

private[actors] class ActorCell(
    val system: ActorSystem,
    val props: Props,
    val dispatcher: MessageDispatcher,
    val self: ActorRef,
    val parent: ActorRef
) extends ActorContext
     with Children
     with Dispatch {

  import Actor._
  import ActorCell._

  private[this] var myActor: Actor = _
  def actor: Actor = myActor
  protected[this] def actor_=(v: Actor): Unit = myActor = v

  private[this] var behaviorStack: List[Receive] = emptyBehaviorStack

  private[this] var currentMessage: Envelope = null

  final def sender: ActorRef = currentMessage match {
    case null                      => system.deadLetters
    case msg if msg.sender ne null => msg.sender
    case _                         => system.deadLetters
  }

  def become(behavior: Actor.Receive, discardOld: Boolean = true): Unit = {
    behaviorStack = behavior :: (
        if (discardOld && behaviorStack.nonEmpty) behaviorStack.tail
        else behaviorStack)
  }

  def unbecome(): Unit = {
    val original = behaviorStack
    behaviorStack =
      if (original.isEmpty || original.tail.isEmpty) actor.receive :: emptyBehaviorStack
      else original.tail
  }

  /*
   * ACTOR INSTANCE HANDLING
   */

  def create(): Unit = {
    actor = newActor()
  }

  //This method is in charge of setting up the contextStack and create a new instance of the Actor
  protected def newActor(): Actor = {
    contextStack = this :: contextStack
    try {
      behaviorStack = emptyBehaviorStack
      val instance = props.newActor()

      if (instance eq null)
        throw ActorInitializationException(self, "Actor instance passed to actorOf can't be 'null'")

      // If no becomes were issued, the actors behavior is its receive method
      behaviorStack =
        if (behaviorStack.isEmpty) instance.receive :: behaviorStack
        else behaviorStack
      instance
    } finally {
      val stackAfter = contextStack
      if (stackAfter.nonEmpty)
        contextStack = (
            if (stackAfter.head eq null) stackAfter.tail.tail
            else stackAfter.tail) // pop null marker plus our context
    }
  }

  /*
   * MESSAGE HANDLING
   */

  final def systemInvoke(message: SystemMessage): Unit = {
    //???
    println(message)
  }

  final def invoke(messageHandle: Envelope): Unit = try {
    currentMessage = messageHandle
    messageHandle.message match {
      //case msg: AutoReceivedMessage => autoReceiveMessage(messageHandle)
      case msg                      => receiveMessage(msg)
    }
    currentMessage = null // reset current message after successful invocation
  } catch {
    case e: Throwable =>
      System.err.println("Error in invoke: " + e)
      handleInvokeFailure(e)
  }

  private def receiveMessage(msg: Any): Unit = {
    behaviorStack.head.applyOrElse(msg, actor.unhandled)
  }

  private def handleInvokeFailure(e: Throwable): Unit = {
    ???
  }
}
