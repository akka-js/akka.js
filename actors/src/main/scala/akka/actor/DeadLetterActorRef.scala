package akka.actor

import akka.event.EventStream
import akka.dispatch.Mailbox
import akka.dispatch.sysmsg._

/**
 * This special dead letter reference has a name: it is that which is returned
 * by a local look-up which is unsuccessful.
 *
 * INTERNAL API
 */
private[akka] class EmptyLocalActorRef(
    override val provider: ActorRefProvider,
    override val path: ActorPath,
    val eventStream: EventStream) extends MinimalActorRef {

  override def sendSystemMessage(message: SystemMessage): Unit = {
    //if (Mailbox.debug) println(s"ELAR $path having enqueued $message")
    specialHandle(message, provider.deadLetters)
  }

  override def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit = message match {
    case null => throw new InvalidMessageException("Message is null")
    case d: DeadLetter =>
      specialHandle(d.message, d.sender) // do NOT form endless loops, since deadLetters will resend!
    case _ if !specialHandle(message, sender) =>
      //eventStream.publish(DeadLetter(message, if (sender eq Actor.noSender) provider.deadLetters else sender, this))
    case _ =>
  }

  protected def specialHandle(msg: Any, sender: ActorRef): Boolean = msg match {
    case w: Watch =>
      if (w.watchee == this && w.watcher != this)
        w.watcher.sendSystemMessage(
          DeathWatchNotification(w.watchee, existenceConfirmed = false, addressTerminated = false))
      true
    case _: Unwatch => true // Just ignore
    case Identify(messageId) =>
      sender ! ActorIdentity(messageId, None)
      true
    /*case s: SelectChildName =>
      s.identifyRequest match {
        case Some(identify) => sender ! ActorIdentity(identify.messageId, None)
        case None =>
          //eventStream.publish(DeadLetter(s.wrappedMessage, if (sender eq Actor.noSender) provider.deadLetters else sender, this))
      }
      true*/
    case _ => false
  }
}

/**
 * Internal implementation of the dead letter destination: will publish any
 * received message to the eventStream, wrapped as [[akka.actor.DeadLetter]].
 *
 * INTERNAL API
 */
private[akka] class DeadLetterActorRef(_provider: ActorRefProvider,
    _path: ActorPath, _eventStream: EventStream)
    extends EmptyLocalActorRef(_provider, _path, _eventStream) {

  override def !(message: Any)(implicit sender: ActorRef = this): Unit = message match {
    case null                => throw new InvalidMessageException("Message is null")
    case Identify(messageId) => sender ! ActorIdentity(messageId, Some(this))
    case d: DeadLetter       =>
      if (!specialHandle(d.message, d.sender))
        ()//eventStream.publish(d)
    case _ =>
      if (!specialHandle(message, sender))
        ()//eventStream.publish(DeadLetter(message, if (sender eq Actor.noSender) provider.deadLetters else sender, this))
  }

  override protected def specialHandle(msg: Any, sender: ActorRef): Boolean = msg match {
    case w: Watch =>
      if (w.watchee != this && w.watcher != this)
        w.watcher.sendSystemMessage(DeathWatchNotification(w.watchee,
            existenceConfirmed = false, addressTerminated = false))
      true
    case _ => super.specialHandle(msg, sender)
  }
}
