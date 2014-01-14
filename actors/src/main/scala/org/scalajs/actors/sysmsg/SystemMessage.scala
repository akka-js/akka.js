package org.scalajs.actors
package sysmsg

private[actors] sealed trait SystemMessage extends PossiblyHarmful

private[actors] sealed trait StashWhenWaitingForChildren

private[actors] sealed trait StashWhenFailed

// sent to self from Dispatcher.register
private[actors] final case class Create(
    failure: Option[ActorInitializationException])
    extends SystemMessage

// sent to self from ActorCell.restart
private[actors] final case class Recreate(cause: Throwable)
    extends SystemMessage with StashWhenWaitingForChildren

// sent to self from ActorCell.suspend
private[actors] final case class Suspend()
    extends SystemMessage with StashWhenWaitingForChildren

// sent to self from ActorCell.resume
private[actors] final case class Resume(
    causedByFailure: Throwable) extends
    SystemMessage with StashWhenWaitingForChildren

// sent to self from ActorCell.stop
private[actors] final case class Terminate()
    extends SystemMessage

// sent to supervisor ActorRef from ActorCell.start
private[actors] final case class Supervise(
    child: ActorRef, async: Boolean)
    extends SystemMessage

// sent to establish a DeathWatch
private[actors] final case class Watch(
    watchee: InternalActorRef, watcher: InternalActorRef)
    extends SystemMessage

// sent to tear down a DeathWatch
private[actors] final case class Unwatch(
    watchee: ActorRef, watcher: ActorRef)
    extends SystemMessage

// switched into the mailbox to signal termination
private[actors] case object NoMessage
    extends SystemMessage

private[actors] final case class Failed(
    child: ActorRef, cause: Throwable, uid: Int)
    extends SystemMessage with StashWhenFailed with StashWhenWaitingForChildren

private[actors] final case class DeathWatchNotification(
  actor: ActorRef,
  existenceConfirmed: Boolean,
  addressTerminated: Boolean) extends SystemMessage
