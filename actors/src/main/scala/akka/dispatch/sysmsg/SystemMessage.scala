package akka.dispatch
package sysmsg

import akka.actor._

private[akka] sealed trait SystemMessage extends PossiblyHarmful

private[akka] sealed trait StashWhenWaitingForChildren

private[akka] sealed trait StashWhenFailed

// sent to self from Dispatcher.register
private[akka] final case class Create(
    failure: Option[ActorInitializationException])
    extends SystemMessage

// sent to self from ActorCell.restart
private[akka] final case class Recreate(cause: Throwable)
    extends SystemMessage with StashWhenWaitingForChildren

// sent to self from ActorCell.suspend
private[akka] final case class Suspend()
    extends SystemMessage with StashWhenWaitingForChildren

// sent to self from ActorCell.resume
private[akka] final case class Resume(
    causedByFailure: Throwable) extends
    SystemMessage with StashWhenWaitingForChildren

// sent to self from ActorCell.stop
private[akka] final case class Terminate()
    extends SystemMessage

// sent to supervisor ActorRef from ActorCell.start
private[akka] final case class Supervise(
    child: ActorRef, async: Boolean)
    extends SystemMessage

// sent to establish a DeathWatch
private[akka] final case class Watch(
    watchee: InternalActorRef, watcher: InternalActorRef)
    extends SystemMessage

// sent to tear down a DeathWatch
private[akka] final case class Unwatch(
    watchee: ActorRef, watcher: ActorRef)
    extends SystemMessage

// switched into the mailbox to signal termination
private[akka] case object NoMessage
    extends SystemMessage

private[akka] final case class Failed(
    child: ActorRef, cause: Throwable, uid: Int)
    extends SystemMessage with StashWhenFailed with StashWhenWaitingForChildren

private[akka] final case class DeathWatchNotification(
  actor: ActorRef,
  existenceConfirmed: Boolean,
  addressTerminated: Boolean) extends SystemMessage
