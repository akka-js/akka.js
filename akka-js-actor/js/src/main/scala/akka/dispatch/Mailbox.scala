/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.dispatch


/** @note IMPLEMENT IN SCALA.JS
import java.util.{ Comparator, PriorityQueue, Queue, Deque }
import java.util.concurrent._
*/
import akka.AkkaException
import akka.dispatch.sysmsg._
import akka.actor.{ ActorCell, ActorRef, Cell, ActorSystem, InternalActorRef, DeadLetter }
/** @note IMPLEMENT IN SCALA.JS
import akka.util.{ Unsafe, BoundedBlockingQueue }
import akka.util.Helpers.ConfigOps
*/
import akka.event.Logging.Error
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import scala.annotation.tailrec
/**@note IMPLEMENT IN SCALA.JS 
import scala.concurrent.forkjoin.ForkJoinTask
*/
import scala.util.control.NonFatal
import com.typesafe.config.Config
/**
 * INTERNAL API
 */
private[akka] object Mailbox {

  type Status = Int

  /*
   * The following assigned numbers CANNOT be changed without looking at the code which uses them!
   */

  // Primary status
  final val Open = 0 // _status is not initialized in AbstractMailbox, so default must be zero! Deliberately without type ascription to make it a compile-time constant
  final val Closed = 1 // Deliberately without type ascription to make it a compile-time constant
  // Secondary status: Scheduled bit may be added to Open/Suspended
  final val Scheduled = 2 // Deliberately without type ascription to make it a compile-time constant
  // Shifted by 2: the suspend count!
  final val shouldScheduleMask = 3
  final val shouldNotProcessMask = ~2
  final val suspendMask = ~3
  final val suspendUnit = 4

  // mailbox debugging helper using println (see below)
  // since this is a compile-time constant, scalac will elide code behind if (Mailbox.debug) (RK checked with 2.9.1)
  final val debug = false // Deliberately without type ascription to make it a compile-time constant
}

/**
 * Mailbox and InternalMailbox is separated in two classes because ActorCell is needed for implementation,
 * but can't be exposed to user defined mailbox subclasses.
 *
 * INTERNAL API
 */
private[akka] abstract class Mailbox(val messageQueue: MessageQueue)
  extends /** @note implement in SCALA.JS extends ForkJoinTask[Unit] with */ SystemMessageQueue with Runnable {

  import Mailbox._

  /*
   * This is needed for actually executing the mailbox, i.e. invoking the
   * ActorCell. There are situations (e.g. RepointableActorRef) where a Mailbox
   * is constructed but we know that we will not execute it, in which case this
   * will be null. It must be a var to support switching into an “active”
   * mailbox, should the owning ActorRef turn local.
   *
   * ANOTHER THING, IMPORTANT:
   *
   * actorCell.start() publishes actorCell & self to the dispatcher, which
   * means that messages may be processed theoretically before self’s constructor
   * ends. The JMM guarantees visibility for final fields only after the end
   * of the constructor, so safe publication requires that THIS WRITE BELOW
   * stay as it is.
   */
  @volatile
  var actor: ActorCell = _
  def setActor(cell: ActorCell): Unit = actor = cell

  def dispatcher: MessageDispatcher = actor.dispatcher

  /**
   * Try to enqueue the message to this queue, or throw an exception.
   */
  def enqueue(receiver: ActorRef, msg: Envelope): Unit = messageQueue.enqueue(receiver, msg)

  /**
   * Try to dequeue the next message from this queue, return null failing that.
   */
  def dequeue(): Envelope = messageQueue.dequeue()

  /**
   * Indicates whether this queue is non-empty.
   */
  def hasMessages: Boolean = messageQueue.hasMessages

  /**
   * Should return the current number of messages held in this queue; may
   * always return 0 if no other value is available efficiently. Do not use
   * this for testing for presence of messages, use `hasMessages` instead.
   */
  def numberOfMessages: Int = messageQueue.numberOfMessages

  @volatile
  protected var _statusDoNotCallMeDirectly: Status = _ //0 by default

  @volatile
  protected var _systemQueueDoNotCallMeDirectly: SystemMessage = _ //null by default

  @inline
  /** @note IMPLEMENT IN SCALA.JS
  final def currentStatus: Mailbox.Status = Unsafe.instance.getIntVolatile(this, AbstractMailbox.mailboxStatusOffset)
  */
  final def currentStatus: Mailbox.Status = _statusDoNotCallMeDirectly

  @inline
  final def shouldProcessMessage: Boolean = (currentStatus & shouldNotProcessMask) == 0

  @inline
  final def suspendCount: Int = currentStatus / suspendUnit

  @inline
  final def isSuspended: Boolean = (currentStatus & suspendMask) != 0

  @inline
  final def isClosed: Boolean = currentStatus == Closed

  @inline
  final def isScheduled: Boolean = (currentStatus & Scheduled) != 0

  @inline
  protected final def updateStatus(oldStatus: Status, newStatus: Status): Boolean = {
    /** @note IMPLEMENT IN SCALA.JS
    Unsafe.instance.compareAndSwapInt(this, AbstractMailbox.mailboxStatusOffset, oldStatus, newStatus)
    */
    _statusDoNotCallMeDirectly = newStatus
    true
  }

  @inline
  protected final def setStatus(newStatus: Status): Unit = 
    /** @note IMPLEMENT IN SCALA.JS
    Unsafe.instance.putIntVolatile(this, AbstractMailbox.mailboxStatusOffset, newStatus)
    */
    _statusDoNotCallMeDirectly = newStatus
  

  /**
   * Reduce the suspend count by one. Caller does not need to worry about whether
   * status was Scheduled or not.
   *
   * @return true if the suspend count reached zero
   */
  @tailrec
  final def resume(): Boolean = currentStatus match {
    case Closed ⇒
      setStatus(Closed); false
    case s ⇒
      val next = if (s < suspendUnit) s else s - suspendUnit
      if (updateStatus(s, next)) next < suspendUnit
      else resume()
  }

  /**
   * Increment the suspend count by one. Caller does not need to worry about whether
   * status was Scheduled or not.
   *
   * @return true if the previous suspend count was zero
   */
  @tailrec
  final def suspend(): Boolean = currentStatus match {
    case Closed ⇒
      setStatus(Closed); false
    case s ⇒
      if (updateStatus(s, s + suspendUnit)) s < suspendUnit
      else suspend()
  }

  /**
   * set new primary status Closed. Caller does not need to worry about whether
   * status was Scheduled or not.
   */
  @tailrec
  final def becomeClosed(): Boolean = currentStatus match {
    case Closed ⇒
      setStatus(Closed); false
    case s ⇒ updateStatus(s, Closed) || becomeClosed()
  }

  /**
   * Set Scheduled status, keeping primary status as is.
   */
  @tailrec
  final def setAsScheduled(): Boolean = {
    val s = currentStatus
    /*
     * Only try to add Scheduled bit if pure Open/Suspended, not Closed or with
     * Scheduled bit already set.
     */
    if ((s & shouldScheduleMask) != Open) false
    else updateStatus(s, s | Scheduled) || setAsScheduled()
  }

  /**
   * Reset Scheduled status, keeping primary status as is.
   */
  @tailrec
  final def setAsIdle(): Boolean = {
    val s = currentStatus
    updateStatus(s, s & ~Scheduled) || setAsIdle()
  }
  /*
   * AtomicReferenceFieldUpdater for system queue.
   */
  protected final def systemQueueGet: LatestFirstSystemMessageList =
    // Note: contrary how it looks, there is no allocation here, as SystemMessageList is a value class and as such
    // it just exists as a typed view during compile-time. The actual return type is still SystemMessage.
    /** @note IMPLEMENT IN SCALA.JS
    new LatestFirstSystemMessageList(Unsafe.instance.getObjectVolatile(this, AbstractMailbox.systemMessageOffset).asInstanceOf[SystemMessage])
    */
    new LatestFirstSystemMessageList(_systemQueueDoNotCallMeDirectly)

  protected final def systemQueuePut(_old: LatestFirstSystemMessageList, _new: LatestFirstSystemMessageList): Boolean = {
    // Note: calling .head is not actually existing on the bytecode level as the parameters _old and _new
    // are SystemMessage instances hidden during compile time behind the SystemMessageList value class.
    // Without calling .head the parameters would be boxed in SystemMessageList wrapper.
    /** @note IMPLEMENT IN SCALA.JS
    Unsafe.instance.compareAndSwapObject(this, AbstractMailbox.systemMessageOffset, _old.head, _new.head)
    */
    _systemQueueDoNotCallMeDirectly = _new.head
    true
  }

  final def canBeScheduledForExecution(hasMessageHint: Boolean, hasSystemMessageHint: Boolean): Boolean = currentStatus match {
    case Open | Scheduled ⇒ hasMessageHint || hasSystemMessageHint || hasSystemMessages || hasMessages
    case Closed           ⇒ false
    case _                ⇒ hasSystemMessageHint || hasSystemMessages
  }

  override final def run(): Unit = {
    try {
      if (!isClosed) { //Volatile read, needed here
        processAllSystemMessages() //First, deal with any system messages
        processMailbox() //Then deal with messages
      }
    } finally {
      setAsIdle() //Volatile write, needed here
      dispatcher.registerForExecution(this, false, false)
    }
  }

  /** override */ final def getRawResult(): Unit = ()
  /** override */ final def setRawResult(unit: Unit): Unit = ()
  /** @note IMPLEMENT IN SCALA.JS
  final override def exec(): Boolean = try { run(); false } catch {
    case ie: InterruptedException ⇒
      Thread.currentThread.interrupt()
      false
    case anything: Throwable ⇒
      val t = Thread.currentThread
      t.getUncaughtExceptionHandler match {
        case null ⇒
        case some ⇒ some.uncaughtException(t, anything)
      }
      throw anything
  }
  */
  final /** override */ def exec(): Boolean = { 
    run()
    false 
  }
  
  /**
   * Process the messages in the mailbox
   */
  @tailrec private final def processMailbox(
    left: Int = java.lang.Math.max(dispatcher.throughput, 1),
    deadlineNs: Long = if (dispatcher.isThroughputDeadlineTimeDefined == true) System.nanoTime + dispatcher.throughputDeadlineTime.toNanos else 0L): Unit =
    if (shouldProcessMessage) {
      val next = dequeue()
      if (next ne null) {
        if (Mailbox.debug) println(actor.self + " processing message " + next)
        actor invoke next
        /**if (Thread.interrupted())
          * throw new InterruptedException("Interrupted while processing actor messages")*/
        processAllSystemMessages()
        if ((left > 1) && ((dispatcher.isThroughputDeadlineTimeDefined == false) || (System.nanoTime - deadlineNs) < 0))
          processMailbox(left - 1, deadlineNs)
      }
    }

  /**
   * Will at least try to process all queued system messages: in case of
   * failure simply drop and go on to the next, because there is nothing to
   * restart here (failure is in ActorCell somewhere …). In case the mailbox
   * becomes closed (because of processing a Terminate message), dump all
   * already dequeued message to deadLetters.
   */
  final def processAllSystemMessages() {
    var interruption: Throwable = null
    var messageList = systemDrain(SystemMessageList.LNil)
    while ((messageList.nonEmpty) && !isClosed) {
      val msg = messageList.head
      messageList = messageList.tail
      msg.unlink()
      if (debug) println(actor.self + " processing system message " + msg + " with " + actor.childrenRefs)
      // we know here that systemInvoke ensures that only "fatal" exceptions get rethrown
      actor systemInvoke msg
      if (Thread.interrupted())
        interruption = new InterruptedException("Interrupted while processing system messages")
      // don’t ever execute normal message when system message present!
      if ((messageList.isEmpty) && !isClosed) messageList = systemDrain(SystemMessageList.LNil)
    }
    /*
     * if we closed the mailbox, we must dump the remaining system messages
     * to deadLetters (this is essential for DeathWatch)
     */
    val dlm = actor.dispatcher.mailboxes.deadLetterMailbox
    while (messageList.nonEmpty) {
      val msg = messageList.head
      messageList = messageList.tail
      msg.unlink()
      try dlm.systemEnqueue(actor.self, msg)
      catch {
        case e: InterruptedException ⇒ interruption = e
        case NonFatal(e) ⇒ actor.system.eventStream.publish(
          Error(e, actor.self.path.toString, this.getClass, "error while enqueuing " + msg + " to deadLetters: " + e.getMessage))
      }
    }
    // if we got an interrupted exception while handling system messages, then rethrow it
    if (interruption ne null) {
      Thread.interrupted() // clear interrupted flag before throwing according to java convention
      throw interruption
    }
  }

  /**
   * Overridable callback to clean up the mailbox,
   * called when an actor is unregistered.
   * By default it dequeues all system messages + messages and ships them to the owning actors' systems' DeadLetterMailbox
   */
  protected[dispatch] def cleanUp(): Unit =
    if (actor ne null) { // actor is null for the deadLetterMailbox
      val dlm = actor.dispatcher.mailboxes.deadLetterMailbox
      var messageList = systemDrain(new LatestFirstSystemMessageList(NoMessage))
      while (messageList.nonEmpty) {
        // message must be “virgin” before being able to systemEnqueue again
        val msg = messageList.head
        messageList = messageList.tail
        msg.unlink()
        dlm.systemEnqueue(actor.self, msg)
      }

      if (messageQueue ne null) // needed for CallingThreadDispatcher, which never calls Mailbox.run()
        messageQueue.cleanUp(actor.self, actor.dispatcher.mailboxes.deadLetterMailbox.messageQueue)
    }
}
