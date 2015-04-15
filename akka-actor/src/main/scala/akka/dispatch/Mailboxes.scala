package akka.dispatch

import akka.actor._
import akka.dispatch.sysmsg.SystemMessage

private[akka] class Mailboxes(
    deadLetters: ActorRef) {

  val deadLetterMailbox: Mailbox = new Mailbox(new MessageQueue {
    def enqueue(receiver: ActorRef, envelope: Envelope): Unit = envelope.message match {
      case _: DeadLetter => // actor subscribing to DeadLetter, drop it
      case msg           => deadLetters.!(DeadLetter(msg, envelope.sender, receiver))(envelope.sender)
    }
    def dequeue() = null
    def hasMessages = false
    def numberOfMessages = 0
    def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = ()
  }) {
    becomeClosed()
    override def systemEnqueue(receiver: ActorRef, handle: SystemMessage): Unit =
      deadLetters ! DeadLetter(handle, receiver, receiver)
    override def hasSystemMessages = false
  }

}
