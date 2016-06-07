/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.dispatch

import com.typesafe.config.{ ConfigFactory, Config }
import akka.actor.{ Actor, ActorSystem }
import akka.event.EventStream
import akka.event.Logging.Warning
import akka.ConfigurationException
import scala.annotation.tailrec
import akka.actor.Props
import scala.util.Try
import scala.util.Failure
import scala.util.control.NonFatal
import akka.actor.ActorRef
import akka.actor.DeadLetter
import akka.actor.DynamicAccess
import akka.dispatch.sysmsg.SystemMessage
import akka.dispatch.sysmsg.LatestFirstSystemMessageList
import akka.dispatch.sysmsg.EarliestFirstSystemMessageList
import akka.dispatch.sysmsg.SystemMessageList

object Mailboxes {
  final val DefaultMailboxId = "akka.actor.default-mailbox"
  final val NoMailboxRequirement = ""
}

private[akka] class Mailboxes(
                               val settings: ActorSystem.Settings,
                               val eventStream: EventStream,
                               dynamicAccess: DynamicAccess,
                               deadLetters: ActorRef) {

  import Mailboxes._

  val deadLetterMailbox: Mailbox = new Mailbox(new MessageQueue {
    def enqueue(receiver: ActorRef, envelope: Envelope): Unit = envelope.message match {
      case _: DeadLetter ⇒ // actor subscribing to DeadLetter, drop it
      case msg           ⇒ deadLetters.tell(DeadLetter(msg, envelope.sender, receiver), envelope.sender)
    }
    def dequeue() = null
    def hasMessages = false
    def numberOfMessages = 0
    def cleanUp(owner: ActorRef, deadLetters: MessageQueue): Unit = ()
  }) {
    becomeClosed()
    override def systemEnqueue(receiver: ActorRef, handle: SystemMessage): Unit =
      deadLetters ! DeadLetter(handle, receiver, receiver)
    def systemDrain(newContents: LatestFirstSystemMessageList): EarliestFirstSystemMessageList = SystemMessageList.ENil
    override def hasSystemMessages = false
  }

   /**
    * Returns a mailbox type as specified in configuration, based on the id, or if not defined None.
    */
   def lookup(id: String): MailboxType = UnboundedMailbox() /** @note IMPLEMENT IN SCALA.JS lookupConfigurator(id) */

   /**
    * Return the required message queue type for this class if any.
    */
    //this dummy implementation should be verified
   def getRequiredType(actorClass: Class[_ <: Actor]): Class[_] = null

   /**
    * Finds out the mailbox type for an actor based on configuration, props and requirements.
    */
   protected[akka] def getMailboxType(props: Props, dispatcherConfig: Config): MailboxType = {
     akka.dispatch.UnboundedMailbox()
   }

   /**
    * Check if this class can have a required message queue type.
    */
    //this dummy implementation should be verified
   def hasRequiredType(actorClass: Class[_ <: Actor]): Boolean = false//rmqClass.isAssignableFrom(actorClass)

}
