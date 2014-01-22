package akka.dispatch

import scala.annotation.tailrec

import akka.actor._
import akka.util.JSQueue

/**
 * A MessageQueue is one of the core components in forming a Mailbox.
 * The MessageQueue is where the normal messages that are sent to Actors will
 * be enqueued (and subsequently dequeued).
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

class NodeMessageQueue extends JSQueue[Envelope] with MessageQueue {

  final def enqueue(receiver: ActorRef, handle: Envelope): Unit =
    enqueue(handle)

  override def dequeue(): Envelope =
    if (isEmpty) null else super.dequeue()

  final def numberOfMessages: Int = size

  final def hasMessages: Boolean = nonEmpty

  @tailrec final def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = {
    val envelope = dequeue()
    if (envelope ne null) {
      deadLetters.enqueue(owner, envelope)
      cleanUp(owner, deadLetters)
    }
  }
}
