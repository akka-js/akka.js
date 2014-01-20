package org.scalajs.actors
package cell

import scala.annotation.tailrec

import scala.util.control.NonFatal
import scala.util.control.Exception.Catcher

import dispatch._
import sysmsg._

private[actors] trait Dispatch { this: ActorCell =>

  private var _mailbox: Mailbox = _

  @inline final def mailbox: Mailbox = _mailbox

  final def swapMailbox(newMailbox: Mailbox): Mailbox = {
    val oldMailbox = mailbox
    _mailbox = newMailbox
    oldMailbox
  }

  final def hasMessages: Boolean = mailbox.hasMessages

  final def numberOfMessages: Int = mailbox.numberOfMessages

  final def isTerminated: Boolean = mailbox.isClosed

  /**
   * Initialize this cell, i.e. set up mailboxes and supervision. The UID must be
   * reasonably different from the previous UID of a possible actor with the same path,
   * which can be achieved by using ThreadLocalRandom.current.nextInt().
   */
  final def init(sendSupervise: Boolean): this.type = {
    /*
     * Create the mailbox and enqueue the Create() message to ensure that
     * this is processed before anything else.
     */
    val mbox = dispatcher.createMailbox(this)

    swapMailbox(mbox)
    mailbox.setActor(this)

    mailbox.systemEnqueue(self, Create(None))

    if (sendSupervise) {
      (parent: InternalActorRef).sendSystemMessage(Supervise(self, async = false))
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
    case NonFatal(e) ⇒
      // TODO publish to eventStream
      //system.eventStream.publish(Error(e, self.path.toString, clazz(actor), "swallowing exception during message send"))
    Console.err.println(s"swallowing exception during message send: $e")
  }

  final def suspend(): Unit = try dispatcher.systemDispatch(this, Suspend()) catch handleException

  final def resume(causedByFailure: Throwable): Unit = try dispatcher.systemDispatch(this, Resume(causedByFailure)) catch handleException

  final def restart(cause: Throwable): Unit = try dispatcher.systemDispatch(this, Recreate(cause)) catch handleException

  final def stop(): Unit = try dispatcher.systemDispatch(this, Terminate()) catch handleException

  def sendMessage(msg: Envelope): Unit =
    try {
      dispatcher.dispatch(this, msg)
    } catch handleException

  def sendSystemMessage(message: SystemMessage): Unit =
    try dispatcher.systemDispatch(this, message) catch handleException

}
