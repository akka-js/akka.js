package akka.dispatch

import scala.annotation.tailrec

import scala.util.control.NonFatal

import akka.actor._
import akka.dispatch.sysmsg.SystemMessage
import akka.util.JSQueue

private[akka] object Mailbox {

  private type Status = Int

  /*
   * The following assigned numbers CANNOT be changed without looking at the
   * code which uses them!
   */

  // Primary status
  private final val Open = 0 // _status is not initialized in Mailbox, so default must be zero! Deliberately without type ascription to make it a compile-time constant
  private final val Closed = 1 // Deliberately without type ascription to make it a compile-time constant
  // Secondary status: Scheduled bit may be added to Open/Suspended
  private final val Scheduled = 2 // Deliberately without type ascription to make it a compile-time constant
  // Shifted by 2: the suspend count!
  private final val shouldScheduleMask = 3
  private final val shouldNotProcessMask = ~2
  private final val suspendMask = ~3
  private final val suspendUnit = 4
}

private[akka] class Mailbox(private[this] val messageQueue: MessageQueue)
    extends Runnable {
  import Mailbox._

  private[this] var _actor: ActorCell = _ // = null
  private[this] var status: Status = _ // = initialize to 0 = Open
  private[this] val systemMessageQueue = new JSQueue[SystemMessage]

  def actor: ActorCell = _actor
  def setActor(actor: ActorCell): Unit = this._actor = actor

  def dispatcher: MessageDispatcher = actor.dispatcher

  /**
   * Try to enqueue the message to this queue, or throw an exception.
   */
  def enqueue(receiver: ActorRef, msg: Envelope): Unit =
    messageQueue.enqueue(receiver, msg)

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

  def systemEnqueue(receiver: ActorRef, msg: SystemMessage): Unit =
    systemMessageQueue.enqueue(msg)

  /**
   * Indicates whether this mailbox has system messages.
   */
  def hasSystemMessages: Boolean = systemMessageQueue.nonEmpty

  @inline
  final def shouldProcessMessage: Boolean = (status & shouldNotProcessMask) == 0

  @inline
  final def suspendCount: Int = status / suspendUnit

  @inline
  final def isSuspended: Boolean = (status & suspendMask) != 0

  @inline
  final def isClosed: Boolean = status == Closed

  @inline
  final def isScheduled: Boolean = (status & Scheduled) != 0

  /**
   * Reduce the suspend count by one. Caller does not need to worry about
   * whether status was Scheduled or not.
   *
   * @return true if the suspend count reached zero
   */
  final def resume(): Boolean = {
    if (status == Closed) false
    else if (status < suspendUnit) true
    else {
      status -= suspendUnit
      status < suspendUnit
    }
  }

  /**
   * Increment the suspend count by one. Caller does not need to worry about
   * whether status was Scheduled or not.
   *
   * @return true if the previous suspend count was zero
   */
  final def suspend(): Boolean = {
    if (status == Closed) false
    else {
      val s = status
      status = s + suspendUnit
      s < suspendUnit
    }
  }

  /**
   * set new primary status Closed. Caller does not need to worry about whether
   * status was Scheduled or not.
   */
  final def becomeClosed(): Boolean = {
    if (status == Closed) false
    else {
      status = Closed
      true
    }
  }

  /**
   * Set Scheduled status, keeping primary status as is.
   */
  final def setAsScheduled(): Boolean = {
    /*
     * Only try to add Scheduled bit if pure Open/Suspended, not Closed or with
     * Scheduled bit already set.
     */
    val s = status
    if ((s & shouldScheduleMask) != Open) false
    else {
      status = s | Scheduled
      true
    }
  }

  /**
   * Reset Scheduled status, keeping primary status as is.
   */
  final def setAsIdle(): Boolean = {
    status &= ~Scheduled
    true
  }

  final def canBeScheduledForExecution(hasMessageHint: Boolean,
      hasSystemMessageHint: Boolean): Boolean = status match {
    case Open | Scheduled =>
      hasMessageHint || hasSystemMessageHint || hasSystemMessages || hasMessages
    case Closed => false
    case _      => hasSystemMessageHint || hasSystemMessages
  }

  final def run(): Unit = {
    try {
      if (!isClosed) {
        processAllSystemMessages() // First, deal with any system messages
        processMailbox() // Then deal with messages
      }
    } finally {
      setAsIdle()
      dispatcher.registerForExecution(this, false, false)
    }
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
        actor invoke next
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
    while (systemMessageQueue.nonEmpty && !isClosed) {
      val msg = systemMessageQueue.dequeue()
      actor systemInvoke msg
    }

    /*
     * if we closed the mailbox, we must dump the remaining system messages
     * to deadLetters (this is essential for DeathWatch)
     */
    val dlm = actor.dispatcher.mailboxes.deadLetterMailbox
    while (systemMessageQueue.nonEmpty) {
      val msg = systemMessageQueue.dequeue()
      try dlm.systemEnqueue(actor.self, msg)
      catch {
        case NonFatal(e) =>
          // TODO publish to event stream
          /*actor.system.eventStream.publish(
            Error(e, actor.self.path.toString, this.getClass,
                "error while enqueuing " + msg + " to deadLetters: " + e.getMessage))*/
          Console.err.println(
              s"error while enqueuing $msg to deadLetters: ${e.getMessage}")
      }
    }
  }

  /**
   * Overridable callback to clean up the mailbox,
   * called when an actor is unregistered.
   * By default it dequeues all system messages + messages and ships them to
   * the owning actors' systems' DeadLetterMailbox
   */
  protected[dispatch] def cleanUp(): Unit = {
    if (actor ne null) { // actor is null for the deadLetterMailbox
      val dlm = actor.dispatcher.mailboxes.deadLetterMailbox
      while (systemMessageQueue.nonEmpty) {
        val msg = systemMessageQueue.dequeue()
        dlm.systemEnqueue(actor.self, msg)
      }

      /*if (messageQueue ne null) // needed for CallingThreadDispatcher, which never calls Mailbox.run()
        messageQueue.cleanUp(actor.self, actor.dispatcher.mailboxes.deadLetterMailbox.messageQueue)*/
    }
  }

}
