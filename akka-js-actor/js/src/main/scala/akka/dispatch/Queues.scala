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
 * Trait to signal that an Actor requires a certain type of message queue semantics.
 *
 * The mailbox type will be looked up by mapping the type T via akka.actor.mailbox.requirements in the config,
 * to a mailbox configuration. If no mailbox is assigned on Props or in deployment config then this one will be used.
 *
 * The queue type of the created mailbox will be checked against the type T and actor creation will fail if it doesn't
 * fulfill the requirements.
 */
trait RequiresMessageQueue[T]

/**
  * DequeBasedMessageQueue refines QueueBasedMessageQueue to be backed by a java.util.Deque.
  */
trait DequeBasedMessageQueueSemantics {
  def enqueueFirst(receiver: ActorRef, handle: Envelope): Unit
}

trait UnboundedDequeBasedMessageQueueSemantics extends DequeBasedMessageQueueSemantics with UnboundedMessageQueueSemantics


/**
  * ControlAwareMessageQueue handles messages that extend [[akka.dispatch.ControlMessage]] with priority.
  */
trait ControlAwareMessageQueueSemantics extends QueueBasedMessageQueue {
  def controlQueue: Queue[Envelope]
  def queue: Queue[Envelope]

  def enqueue(receiver: ActorRef, handle: Envelope): Unit = handle match {
    case envelope @ Envelope(_: ControlMessage, _) ⇒ controlQueue add envelope
    case envelope                                  ⇒ queue add envelope
  }

  def dequeue(): Envelope = {
    val controlMsg = controlQueue.poll()

    if (controlMsg ne null) controlMsg
    else queue.poll()
  }

  override def numberOfMessages: Int = controlQueue.size + queue.size

  override def hasMessages: Boolean = !(queue.isEmpty && controlQueue.isEmpty)
}

trait UnboundedControlAwareMessageQueueSemantics extends UnboundedMessageQueueSemantics with ControlAwareMessageQueueSemantics

/**
  * Messages that extend this trait will be handled with priority by control aware mailboxes.
  */
trait ControlMessage