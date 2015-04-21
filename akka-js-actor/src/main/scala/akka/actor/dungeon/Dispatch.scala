/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.actor.dungeon

import scala.annotation.tailrec
import akka.dispatch.{MailboxType, MessageDispatcher, Mailbox, Envelope}
import akka.dispatch.sysmsg._
import akka.event.Logging.Error
/**
 * @note IMPLEMENT IN SCALA.JS
 *
 import akka.util.Unsafe
 */
import akka.actor._
/**
 * @note IMPLEMENT IN SCALA.JS
 *
 import akka.serialization.SerializationExtension
 */
import scala.util.control.NonFatal
import scala.util.control.Exception.Catcher
/**
 * @note IMPLEMENT IN SCALA.JS
 *
 import akka.dispatch.MailboxType
 import akka.dispatch.ProducesMessageQueue
 */

private[akka] trait Dispatch { this: ActorCell ⇒

  @volatile private var _mailboxDoNotCallMeDirectly: Mailbox = _ //This must be volatile since it isn't protected by the mailbox status

/**
 * @note IMPLEMENT IN SCALA.JS
 *
   //@inline final def mailbox: Mailbox = Unsafe.instance.getObjectVolatile(this, AbstractActorCell.mailboxOffset).asInstanceOf[Mailbox]
 */
  @inline final def mailbox: Mailbox = _mailboxDoNotCallMeDirectly

  /**
   * @note IMPLEMENT IN SCALA.JS
   *
   //@tailrec final def swapMailbox(newMailbox: Mailbox): Mailbox = {
     val oldMailbox = mailbox
     if (!Unsafe.instance.compareAndSwapObject(this, AbstractActorCell.mailboxOffset, oldMailbox, newMailbox)) swapMailbox(newMailbox)
     else oldMailbox
   }
   */
  final def swapMailbox(newMailbox: Mailbox): Mailbox = {
    _mailboxDoNotCallMeDirectly = newMailbox
    newMailbox
  }

  final def hasMessages: Boolean = mailbox.hasMessages

  final def numberOfMessages: Int = mailbox.numberOfMessages

  final def isTerminated: Boolean = mailbox.isClosed

  /**
   * Initialize this cell, i.e. set up mailboxes and supervision. The UID must be
   * reasonably different from the previous UID of a possible actor with the same path,
   * which can be achieved by using ThreadLocalRandom.current.nextInt().
   */
  final def init(sendSupervise: Boolean/** @note IMPLEMENT IN SCALA.JS , mailboxType: MailboxType */): this.type = {
    /*
     * Create the mailbox and enqueue the Create() message to ensure that
     * this is processed before anything else.
     */
    val mbox = dispatcher.createMailbox(this/** @note IMPLEMENT IN SCALA.JS , mailboxType */)

    /*
     * The mailboxType was calculated taking into account what the MailboxType
     * has promised to produce. If that was more than the default, then we need
     * to reverify here because the dispatcher may well have screwed it up.
     */
    // we need to delay the failure to the point of actor creation so we can handle
    // it properly in the normal way
    /**
     * @note IMPLEMENT IN SCALA.JS
     *
         val actorClass = props.actorClass
         val createMessage = mailboxType match {
           case _: ProducesMessageQueue[_] if system.mailboxes.hasRequiredType(actorClass) ⇒
             val req = system.mailboxes.getRequiredType(actorClass)
             if (req isInstance mbox.messageQueue) Create(None)
             else {
               val gotType = if (mbox.messageQueue == null) "null" else mbox.messageQueue.getClass.getName
               Create(Some(ActorInitializationException(self,
                 s"Actor [$self] requires mailbox type [$req] got [$gotType]")))
             }
           case _ ⇒ Create(None)
         }
     */

    swapMailbox(mbox)
    mailbox.setActor(this)

    // ➡➡➡ NEVER SEND THE SAME SYSTEM MESSAGE OBJECT TO TWO ACTORS ⬅⬅⬅
    /**
     * @note IMPLEMENT IN SCALA.JS
     *
     mailbox.systemEnqueue(self, createMessage)
     */
    mailbox.systemEnqueue(self, Create(None))

    if (sendSupervise) {
      // ➡➡➡ NEVER SEND THE SAME SYSTEM MESSAGE OBJECT TO TWO ACTORS ⬅⬅⬅
      parent.sendSystemMessage(akka.dispatch.sysmsg.Supervise(self, async = false))
    }
    this
  }

  /**
   * Start this cell, i.e. attach it to the dispatcher.
   */
  def start(): this.type = {
    // This call is expected to start off the actor by scheduling its mailbox.
    dispatcher.attach(this)
    this
  }

  private def handleException: Catcher[Unit] = {
    case e: InterruptedException ⇒
      system.eventStream.publish(Error(e, self.path.toString, clazz(actor), "interrupted during message send"))
      /**
       * @note IMPLEMENT IN SCALA.JS
       *
       Thread.currentThread.interrupt()
       */
    case NonFatal(e) ⇒
      system.eventStream.publish(Error(e, self.path.toString, clazz(actor), "swallowing exception during message send"))
  }

  // ➡➡➡ NEVER SEND THE SAME SYSTEM MESSAGE OBJECT TO TWO ACTORS ⬅⬅⬅
  final def suspend(): Unit = try dispatcher.systemDispatch(this, Suspend()) catch handleException

  // ➡➡➡ NEVER SEND THE SAME SYSTEM MESSAGE OBJECT TO TWO ACTORS ⬅⬅⬅
  final def resume(causedByFailure: Throwable): Unit = try dispatcher.systemDispatch(this, Resume(causedByFailure)) catch handleException

  // ➡➡➡ NEVER SEND THE SAME SYSTEM MESSAGE OBJECT TO TWO ACTORS ⬅⬅⬅
  final def restart(cause: Throwable): Unit = try dispatcher.systemDispatch(this, Recreate(cause)) catch handleException

  // ➡➡➡ NEVER SEND THE SAME SYSTEM MESSAGE OBJECT TO TWO ACTORS ⬅⬅⬅
  final def stop(): Unit = try dispatcher.systemDispatch(this, Terminate()) catch handleException

  def sendMessage(msg: Envelope): Unit =
    try {
      /**
       * @note IMPLEMENT IN SCALA.JS
       *
       if (system.settings.SerializeAllMessages) {
         val unwrapped = (msg.message match {
           case DeadLetter(wrapped, _, _) ⇒ wrapped
           case other                     ⇒ other
         }).asInstanceOf[AnyRef]
         if (!unwrapped.isInstanceOf[NoSerializationVerificationNeeded]) {
           val s = SerializationExtension(system)
           s.deserialize(s.serialize(unwrapped).get, unwrapped.getClass).get
         }
       }
       */
      dispatcher.dispatch(this, msg)
    } catch handleException

  override def sendSystemMessage(message: SystemMessage): Unit = try dispatcher.systemDispatch(this, message) catch handleException

}
