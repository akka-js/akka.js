package akka.actor

import scala.annotation.{tailrec, switch}

import scala.collection.immutable
import scala.util.control.NonFatal

import akka.dispatch.MessageDispatcher
import akka.event.Logging._
import akka.dispatch.sysmsg._

private[akka] object ActorCell {
  var contextStack: List[ActorContext] = Nil

  final val emptyBehaviorStack: List[Actor.Receive] = Nil

  final val emptyActorRefSet: Set[ActorRef] = immutable.Set.empty

  final val undefinedUid = 0

  @tailrec final def newUid(): Int = {
    // Note that this uid is also used as hashCode in ActorRef, so be careful
    // to not break hashing if you change the way uid is generated
    val uid = scala.util.Random.nextInt()
    if (uid == undefinedUid) newUid()
    else uid
  }

  final def splitNameAndUid(name: String): (String, Int) = {
    val i = name.indexOf('#')
    if (i < 0) (name, undefinedUid)
    else (name.substring(0, i), Integer.valueOf(name.substring(i + 1)))
  }

  final val emptyCancellable: Cancellable = new Cancellable {
    def isCancelled: Boolean = false
    def cancel(): Boolean = false
  }

  final val DefaultState = 0
  final val SuspendedState = 1
  final val SuspendedWaitForChildrenState = 2
}

private[akka] class ActorCell(
    val system: ActorSystem,
    val props: Props,
    val dispatcher: MessageDispatcher,
    val self: ActorRef,
    _parent: ActorRef
) extends ActorContext
     with dungeon.Children
     with dungeon.Dispatch
     with dungeon.ReceiveTimeout
     with dungeon.DeathWatch
     with dungeon.FaultHandling {

  import Actor._
  import ActorCell._

  val parent: InternalActorRef = _parent

  final def systemImpl = system.asInstanceOf[ActorSystemImpl]
  protected final def guardian = self   // huh??
  protected final def lookupRoot = self // huh??
  final def provider = system.provider

  protected def uid: Int = self.path.uid
  private[this] var myActor: Actor = _
  def actor: Actor = myActor
  protected[this] def actor_=(v: Actor): Unit = myActor = v

  private[this] var behaviorStack: List[Receive] = emptyBehaviorStack
  private[this] var sysmsgStashLatestFirst: List[SystemMessage] = Nil

  protected[this] var currentMessage: Envelope = null

  protected def stash(msg: SystemMessage): Unit = {
    sysmsgStashLatestFirst ::= msg
  }

  private def unstashAllLatestFirst(): List[SystemMessage] = {
    val unstashed = sysmsgStashLatestFirst
    sysmsgStashLatestFirst = Nil
    unstashed
  }

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
   * MESSAGE HANDLING
   */

  final def systemInvoke(message: SystemMessage): Unit = {
    /*
     * When recreate/suspend/resume are received while restarting (i.e. between
     * preRestart and postRestart, waiting for children to terminate), these
     * must not be executed immediately, but instead queued and released after
     * finishRecreate returns. This can only ever be triggered by
     * ChildTerminated, and ChildTerminated is not one of the queued message
     * types (hence the overwrite further down). Mailbox sets message.next=null
     * before systemInvoke, so this will only be non-null during such a replay.
     */

    def calculateState: Int =
      if (waitingForChildrenOrNull ne null) SuspendedWaitForChildrenState
      else if (mailbox.isSuspended) SuspendedState
      else DefaultState

    @tailrec def sendAllToDeadLetters(messages: List[SystemMessage]): Unit =
      if (messages.nonEmpty) {
        val tail = messages.tail
        val msg = messages.head
        system.deadLetters ! msg
        sendAllToDeadLetters(tail)
      }

    def shouldStash(m: SystemMessage, state: Int): Boolean =
      (state: @switch) match {
        case DefaultState                  ⇒ false
        case SuspendedState                ⇒ m.isInstanceOf[StashWhenFailed]
        case SuspendedWaitForChildrenState ⇒ m.isInstanceOf[StashWhenWaitingForChildren]
      }

    @tailrec
    def invokeAll(messages: List[SystemMessage], currentState: Int): Unit = {
      val rest = messages.tail
      val message = messages.head
      try {
        message match {
          case message: SystemMessage if shouldStash(message, currentState) => stash(message)
          case f: Failed => handleFailure(f)
          case DeathWatchNotification(a, ec, at) => watchedActorTerminated(a, ec, at)
          case Create(failure) => create(failure)
          case Watch(watchee, watcher) => addWatcher(watchee, watcher)
          case Unwatch(watchee, watcher) => remWatcher(watchee, watcher)
          case Recreate(cause) => faultRecreate(cause)
          case Suspend() => faultSuspend()
          case Resume(inRespToFailure) => faultResume(inRespToFailure)
          case Terminate() => terminate()
          case Supervise(child, async) => supervise(child, async)
          case NoMessage => // only here to suppress warning
        }
      } catch handleNonFatalOrInterruptedException { e =>
        handleInvokeFailure(Nil, e)
      }
      val newState = calculateState
      // As each state accepts a strict subset of another state, it is enough
      // to unstash if we "walk up" the state chain
      val todo =
        if (newState < currentState) unstashAllLatestFirst() reverse_::: rest
        else rest

      if (isTerminated) sendAllToDeadLetters(todo)
      else if (todo.nonEmpty) invokeAll(todo, newState)
    }

    invokeAll(List(message), calculateState)
  }

  final def invoke(messageHandle: Envelope): Unit = try {
    currentMessage = messageHandle
    cancelReceiveTimeout() // FIXME: leave this here???
    messageHandle.message match {
      case msg: AutoReceivedMessage => autoReceiveMessage(messageHandle)
      case msg                      => receiveMessage(msg)
    }
    currentMessage = null // reset current message after successful invocation
  } catch handleNonFatalOrInterruptedException { e =>
    handleInvokeFailure(Nil, e)
  } finally {
    checkReceiveTimeout() // Reschedule receive timeout
  }

  def autoReceiveMessage(msg: Envelope): Unit = {
    msg.message match {
      case t: Terminated              => receivedTerminated(t)
      //case AddressTerminated(address) => addressTerminated(address)
      case Kill                       => throw new ActorKilledException("Kill")
      case PoisonPill                 => (self: InternalActorRef).stop()
      /*case SelectParent(m) =>
        if (self == system.provider.rootGuardian) self.tell(m, msg.sender)
        else parent.tell(m, msg.sender)
      case s @ SelectChildName(name, m) =>
        def selectChild(): Unit = {
          getChildByName(name) match {
            case Some(c: ChildRestartStats) => c.child.tell(m, msg.sender)
            case _ ⇒
              s.identifyRequest foreach { x => sender ! ActorIdentity(x.messageId, None) }
          }
        }
        // need this special case because of extraNames handled by rootGuardian
        if (self == system.provider.rootGuardian) {
          self.asInstanceOf[LocalActorRef].getSingleChild(name) match {
            case Nobody => selectChild()
            case child  => child.tell(m, msg.sender)
          }
        } else
          selectChild()
      case SelectChildPattern(p, m) => for (c <- children if p.matcher(c.path.name).matches) c.tell(m, msg.sender)*/
      case Identify(messageId)      => sender ! ActorIdentity(messageId, Some(self))
    }
  }

  protected def receiveMessage(msg: Any): Unit = {
    behaviorStack.head.applyOrElse(msg, actor.unhandled)
  }

  /*
   * ACTOR INSTANCE HANDLING
   */

  //This method is in charge of setting up the contextStack and create a new instance of the Actor
  protected def newActor(): Actor = {
    contextStack = this :: contextStack
    try {
      behaviorStack = emptyBehaviorStack
      val instance = props.newActor()

      if (instance eq null)
        throw ActorInitializationException(self,
            "Actor instance passed to actorOf can't be 'null'")

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

  protected def create(failure: Option[ActorInitializationException]): Unit = {
    def clearOutActorIfNonNull(): Unit = {
      if (actor ne null) {
        clearActorFields(actor)
        actor = null // ensure that we know that we failed during creation
      }
    }

    failure foreach { throw _ }

    try {
      val created = newActor()
      actor = created
      created.preStart()
      checkReceiveTimeout()
    } catch {
      case NonFatal(e) =>
        clearOutActorIfNonNull()
        e match {
          case i: InstantiationException => throw ActorInitializationException(self,
            """exception during creation, this problem is likely to occur because the class of the Actor you tried to create is either,
               a non-static inner class (in which case make it a static inner class or use Props(new ...) or Props( new UntypedActorFactory ... )
               or is missing an appropriate, reachable no-args constructor.
              """, i.getCause)
          case x => throw ActorInitializationException(self, "exception during creation", x)
        }
    }
  }

  private def supervise(child: ActorRef, async: Boolean): Unit = {
    // huh? This is totally useless
    /*if (!isTerminating) {
      if (childStatsByName(child.path.name).isDefined) {
        initChild(child)
        if (system.settings.DebugLifecycle)
          publish(Debug(self.path.toString, clazz(actor), "now supervising " + child))
      } else {
        publish(Error(self.path.toString, clazz(actor), "received Supervise from unregistered child " + child + ", this will not end well"))
      }
    }*/
  }

  final protected def clearActorCellFields(cell: ActorCell): Unit = {
    cell.unstashAllLatestFirst()
    //if (!lookupAndSetField(cell.getClass, cell, "props", ActorCell.terminatedProps))
    //  throw new IllegalArgumentException("ActorCell has no props field")
  }

  final protected def clearActorFields(actorInstance: Actor): Unit = {
    actorInstance.setActorFields(context = null, self = system.deadLetters)
    currentMessage = null
    behaviorStack = emptyBehaviorStack
  }

  // logging is not the main purpose, and if it fails there’s nothing we can do
  protected final def publish(e: LogEvent): Unit =
    try system.eventStream.publish(e)
    catch { case NonFatal(_) => }

  protected final def clazz(o: AnyRef): Class[_] =
    if (o eq null) this.getClass else o.getClass
}
