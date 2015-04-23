package akka.dispatch

import akka.AkkaException
import akka.dispatch.sysmsg._
import akka.actor.{ ActorCell, ActorRef, Cell, ActorSystem, InternalActorRef, DeadLetter }
import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import akka.dispatch.{ AbstractNodeQueue => Queue }
import com.typesafe.config.Config

/**
 * A MessageQueue is one of the core components in forming an Akka Mailbox.
 * The MessageQueue is where the normal messages that are sent to Actors will be enqueued (and subsequently dequeued)
 * It needs to at least support N producers and 1 consumer thread-safely.
 */
trait MessageQueue {
  /**
   * Try to enqueue the message to this queue, or throw an exception.
   */
  def enqueue(receiver: ActorRef, handle: Envelope): Unit // NOTE: receiver is used only in two places, but cannot be removed

  /**
   * Try to dequeue the next message from this queue, return null failing that.
   */
  def dequeue(): Envelope

  /**
   * Should return the current number of messages held in this queue; may
   * always return 0 if no other value is available efficiently. Do not use
   * this for testing for presence of messages, use `hasMessages` instead.
   */
  def numberOfMessages: Int

  /**
   * Indicates whether this queue is non-empty.
   */
  def hasMessages: Boolean

  /**
   * Called when the mailbox this queue belongs to is disposed of. Normally it
   * is expected to transfer all remaining messages into the dead letter queue
   * which is passed in. The owner of this MessageQueue is passed in if
   * available (e.g. for creating DeadLetters()), “/deadletters” otherwise.
   */
  def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit
}

class NodeMessageQueue extends AbstractNodeQueue[Envelope] with MessageQueue with UnboundedMessageQueueSemantics {

  final def enqueue(receiver: ActorRef, handle: Envelope): Unit = add(handle)

  override final def dequeue(): Envelope = poll()

  final def numberOfMessages: Int = count()

  final def hasMessages: Boolean = !isEmpty

  @tailrec final def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
    val envelope = dequeue()
    if (envelope ne null) {
      deadLetters.enqueue(owner, envelope)
      cleanUp(owner, deadLetters)
    }
  }
}

/**
 * INTERNAL API
 */
private[akka] trait SystemMessageQueue {
  /**
   * Enqueue a new system message, e.g. by prepending atomically as new head of a single-linked list.
   */
  def systemEnqueue(receiver: ActorRef, message: SystemMessage): Unit

  /**
   * Dequeue all messages from system queue and return them as single-linked list.
   */
  def systemDrain(newContents: LatestFirstSystemMessageList): EarliestFirstSystemMessageList

  def hasSystemMessages: Boolean
}

/**
 * INTERNAL API
 */
private[akka] trait DefaultSystemMessageQueue { self: Mailbox ⇒

  @tailrec
  final def systemEnqueue(receiver: ActorRef, message: SystemMessage): Unit = {
    assert(message.unlinked)
    if (Mailbox.debug) println(receiver + " having enqueued " + message)
    val currentList = systemQueueGet
    if (currentList.head == NoMessage) {
      if (actor ne null) actor.dispatcher.mailboxes.deadLetterMailbox.systemEnqueue(receiver, message)
    } else {
      if (!systemQueuePut(currentList, message :: currentList)) {
        message.unlink()
        systemEnqueue(receiver, message)
      }
    }
  }

  @tailrec
  final def systemDrain(newContents: LatestFirstSystemMessageList): EarliestFirstSystemMessageList = {
    val currentList = systemQueueGet
    if (currentList.head == NoMessage) new EarliestFirstSystemMessageList(null)
    else if (systemQueuePut(currentList, newContents)) currentList.reverse
    else systemDrain(newContents)
  }

  def hasSystemMessages: Boolean = systemQueueGet.head match {
    case null | NoMessage ⇒ false
    case _                ⇒ true
  }

}

/**
 * This is a marker trait for message queues which support multiple consumers,
 * as is required by the BalancingDispatcher.
 */
trait MultipleConsumerSemantics

/**
 * A QueueBasedMessageQueue is a MessageQueue backed by a java.util.Queue.
 */
trait QueueBasedMessageQueue extends MessageQueue with MultipleConsumerSemantics {
  def queue: Queue[Envelope]
  def numberOfMessages = queue.size
  def hasMessages = !queue.isEmpty
  def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
    if (hasMessages) {
      var envelope = dequeue
      while (envelope ne null) {
        deadLetters.enqueue(owner, envelope)
        envelope = dequeue
      }
    }
  }
}

/**
 * UnboundedMessageQueueSemantics adds unbounded semantics to a QueueBasedMessageQueue,
 * i.e. a non-blocking enqueue and dequeue.
 */
trait UnboundedMessageQueueSemantics

trait UnboundedQueueBasedMessageQueue extends QueueBasedMessageQueue with UnboundedMessageQueueSemantics {
  def enqueue(receiver: ActorRef, handle: Envelope): Unit = queue add handle
  //def dequeue(): Envelope = queue.poll()
}

/**
 * BoundedMessageQueueSemantics adds bounded semantics to a QueueBasedMessageQueue,
 * i.e. blocking enqueue with timeout.
 */
/**
trait BoundedMessageQueueSemantics {
  def pushTimeOut: Duration
}

trait BoundedQueueBasedMessageQueue extends QueueBasedMessageQueue with BoundedMessageQueueSemantics {
  override def queue: BlockingQueue[Envelope]

  def enqueue(receiver: ActorRef, handle: Envelope): Unit =
    if (pushTimeOut.length >= 0) {
      if (!queue.offer(handle, pushTimeOut.length, pushTimeOut.unit))
        receiver.asInstanceOf[InternalActorRef].provider.deadLetters.tell(
          DeadLetter(handle.message, handle.sender, receiver), handle.sender)
    } else queue put handle

  def dequeue(): Envelope = queue.poll()
}

/**
 * DequeBasedMessageQueue refines QueueBasedMessageQueue to be backed by a java.util.Deque.
 */
trait DequeBasedMessageQueueSemantics {
  def enqueueFirst(receiver: ActorRef, handle: Envelope): Unit
}

trait UnboundedDequeBasedMessageQueueSemantics extends DequeBasedMessageQueueSemantics with UnboundedMessageQueueSemantics

trait BoundedDequeBasedMessageQueueSemantics extends DequeBasedMessageQueueSemantics with BoundedMessageQueueSemantics

trait DequeBasedMessageQueue extends QueueBasedMessageQueue with DequeBasedMessageQueueSemantics {
  def queue: Deque[Envelope]
}

/**
 * UnboundedDequeBasedMessageQueueSemantics adds unbounded semantics to a DequeBasedMessageQueue,
 * i.e. a non-blocking enqueue and dequeue.
 */
trait UnboundedDequeBasedMessageQueue extends DequeBasedMessageQueue with UnboundedDequeBasedMessageQueueSemantics {
  def enqueue(receiver: ActorRef, handle: Envelope): Unit = queue add handle
  def enqueueFirst(receiver: ActorRef, handle: Envelope): Unit = queue addFirst handle
  def dequeue(): Envelope = queue.poll()
}

/**
 * BoundedMessageQueueSemantics adds bounded semantics to a DequeBasedMessageQueue,
 * i.e. blocking enqueue with timeout.
 */
trait BoundedDequeBasedMessageQueue extends DequeBasedMessageQueue with BoundedDequeBasedMessageQueueSemantics {
  def pushTimeOut: Duration
  override def queue: BlockingDeque[Envelope]

  def enqueue(receiver: ActorRef, handle: Envelope): Unit =
    if (pushTimeOut.length >= 0) {
      if (!queue.offer(handle, pushTimeOut.length, pushTimeOut.unit))
        receiver.asInstanceOf[InternalActorRef].provider.deadLetters.tell(
          DeadLetter(handle.message, handle.sender, receiver), handle.sender)
    } else queue put handle

  def enqueueFirst(receiver: ActorRef, handle: Envelope): Unit =
    if (pushTimeOut.length >= 0) {
      if (!queue.offerFirst(handle, pushTimeOut.length, pushTimeOut.unit))
        receiver.asInstanceOf[InternalActorRef].provider.deadLetters.tell(
          DeadLetter(handle.message, handle.sender, receiver), handle.sender)
    } else queue putFirst handle

  def dequeue(): Envelope = queue.poll()
}
*/
/**
 * MailboxType is a factory to create MessageQueues for an optionally
 * provided ActorContext.
 *
 * <b>Possibly Important Notice</b>
 *
 * When implementing a custom mailbox type, be aware that there is special
 * semantics attached to `system.actorOf()` in that sending to the returned
 * ActorRef may—for a short period of time—enqueue the messages first in a
 * dummy queue. Top-level actors are created in two steps, and only after the
 * guardian actor has performed that second step will all previously sent
 * messages be transferred from the dummy queue into the real mailbox.
 */
trait MailboxType {
  def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue
}

trait ProducesMessageQueue[T <: MessageQueue]

/**
 * UnboundedMailbox is the default unbounded MailboxType used by Akka Actors.
 */
case class UnboundedMailbox() extends MailboxType with ProducesMessageQueue[UnboundedMailbox.MessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = this()

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue =
    new UnboundedMailbox.MessageQueue
}

object UnboundedMailbox {
  class MessageQueue extends AbstractNodeQueue[Envelope] with UnboundedQueueBasedMessageQueue {
    final def queue: Queue[Envelope] = this
    override def dequeue(): Envelope = queue.poll()
  }
}

/**
 * SingleConsumerOnlyUnboundedMailbox is a high-performance, multiple producer—single consumer, unbounded MailboxType,
 * the only drawback is that you can't have multiple consumers,
 * which rules out using it with BalancingPool (BalancingDispatcher) for instance.
 */
case class SingleConsumerOnlyUnboundedMailbox() extends MailboxType with ProducesMessageQueue[NodeMessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = this()

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue = new NodeMessageQueue
}

/**
 * BoundedMailbox is the default bounded MailboxType used by Akka Actors.
 */
/** @note IMPLEMENT IN SCALA.JS
case class BoundedMailbox(val capacity: Int, val pushTimeOut: FiniteDuration)
  extends MailboxType with ProducesMessageQueue[BoundedMailbox.MessageQueue] {

  /** @note IMPLEMENT IN SCALA.JS
  def this(settings: ActorSystem.Settings, config: Config) = this(config.getInt("mailbox-capacity"),
    config.getNanosDuration("mailbox-push-timeout-time"))
  */

  if (capacity < 0) throw new IllegalArgumentException("The capacity for BoundedMailbox can not be negative")
  if (pushTimeOut eq null) throw new IllegalArgumentException("The push time-out for BoundedMailbox can not be null")

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue =
    new BoundedMailbox.MessageQueue(capacity, pushTimeOut)
}

object BoundedMailbox {
  class MessageQueue(capacity: Int, final val pushTimeOut: FiniteDuration)
    extends LinkedBlockingQueue[Envelope](capacity) with BoundedQueueBasedMessageQueue {
    final def queue: BlockingQueue[Envelope] = this
  }
}

/**
 * UnboundedPriorityMailbox is an unbounded mailbox that allows for prioritization of its contents.
 * Extend this class and provide the Comparator in the constructor.
 */
class UnboundedPriorityMailbox(val cmp: Comparator[Envelope], val initialCapacity: Int)
  extends MailboxType with ProducesMessageQueue[UnboundedPriorityMailbox.MessageQueue] {
  def this(cmp: Comparator[Envelope]) = this(cmp, 11)
  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue =
    new UnboundedPriorityMailbox.MessageQueue(initialCapacity, cmp)
}

object UnboundedPriorityMailbox {
  class MessageQueue(initialCapacity: Int, cmp: Comparator[Envelope])
    extends PriorityBlockingQueue[Envelope](initialCapacity, cmp) with UnboundedQueueBasedMessageQueue {
    final def queue: Queue[Envelope] = this
  }
}

/**
 * BoundedPriorityMailbox is a bounded mailbox that allows for prioritization of its contents.
 * Extend this class and provide the Comparator in the constructor.
 */
class BoundedPriorityMailbox( final val cmp: Comparator[Envelope], final val capacity: Int, final val pushTimeOut: Duration)
  extends MailboxType with ProducesMessageQueue[BoundedPriorityMailbox.MessageQueue] {

  if (capacity < 0) throw new IllegalArgumentException("The capacity for BoundedMailbox can not be negative")
  if (pushTimeOut eq null) throw new IllegalArgumentException("The push time-out for BoundedMailbox can not be null")

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue =
    new BoundedPriorityMailbox.MessageQueue(capacity, cmp, pushTimeOut)
}

object BoundedPriorityMailbox {
  class MessageQueue(capacity: Int, cmp: Comparator[Envelope], val pushTimeOut: Duration)
    extends BoundedBlockingQueue[Envelope](capacity, new PriorityQueue[Envelope](11, cmp))
    with BoundedQueueBasedMessageQueue {
    final def queue: BlockingQueue[Envelope] = this
  }
}


/**
 * UnboundedDequeBasedMailbox is an unbounded MailboxType, backed by a Deque.
 */
case class UnboundedDequeBasedMailbox() extends MailboxType with ProducesMessageQueue[UnboundedDequeBasedMailbox.MessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = this()

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue =
    new UnboundedDequeBasedMailbox.MessageQueue
}

object UnboundedDequeBasedMailbox {
  class MessageQueue extends LinkedBlockingDeque[Envelope] with UnboundedDequeBasedMessageQueue {
    final val queue = this
  }
}

/**
 * BoundedDequeBasedMailbox is an bounded MailboxType, backed by a Deque.
 */
case class BoundedDequeBasedMailbox( final val capacity: Int, final val pushTimeOut: FiniteDuration)
  extends MailboxType with ProducesMessageQueue[BoundedDequeBasedMailbox.MessageQueue] {

  /** @note IMPLEMENT IN SCALA.JS
  def this(settings: ActorSystem.Settings, config: Config) = this(config.getInt("mailbox-capacity"),
    config.getNanosDuration("mailbox-push-timeout-time"))
  */

  if (capacity < 0) throw new IllegalArgumentException("The capacity for BoundedDequeBasedMailbox can not be negative")
  if (pushTimeOut eq null) throw new IllegalArgumentException("The push time-out for BoundedDequeBasedMailbox can not be null")

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue =
    new BoundedDequeBasedMailbox.MessageQueue(capacity, pushTimeOut)
}

object BoundedDequeBasedMailbox {
  class MessageQueue(capacity: Int, val pushTimeOut: FiniteDuration)
    extends LinkedBlockingDeque[Envelope](capacity) with BoundedDequeBasedMessageQueue {
    final val queue = this
  }
}
*/
/**
 * Trait to signal that an Actor requires a certain type of message queue semantics.
 *
 * The mailbox type will be looked up by mapping the type T via akka.actor.mailbox.requirements in the config,
 * to a mailbox configuration. If no mailbox is assigned on Props or in deployment config then this one will be used.
 *
 * The queue type of the created mailbox will be checked against the type T and actor creation will fail if it doesn't
 * fulfill the requirements.
 */
trait RequiresMessageQueue[T]