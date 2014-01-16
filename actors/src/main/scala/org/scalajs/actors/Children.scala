package org.scalajs.actors

import scala.annotation.tailrec
import scala.collection.{ immutable, mutable }

import org.scalajs.actors.util.Helpers

import ActorPath.ElementRegex

private[actors] object Children {

  private final val sharedEmptyChildrenRefs: JSMap[ChildRestartStats] = JSMap.empty

  sealed trait SuspendReason
  sealed trait WaitingForChildren extends SuspendReason
  case object UserRequest extends SuspendReason
  // careful with those system messages, all handling to be taking place in ActorCell.scala!
  final case class Recreation(cause: Throwable) extends SuspendReason with WaitingForChildren
  final case class Creation() extends SuspendReason with WaitingForChildren
  case object Termination extends SuspendReason

}

private[actors] trait Children { this: ActorCell =>

  import Children._

  private[this] var childrenRefs: JSMap[ChildRestartStats] = sharedEmptyChildrenRefs
  private[this] var suspendReason: SuspendReason = _ // default = null
  private[this] var terminating: Boolean = _ // default = false

  final def children: immutable.Iterable[ActorRef] =
    childrenRefs.values.map(_.child).toList

  final def child(name: String): Option[ActorRef] =
    childrenRefs.get(name).map(_.child)

  final def childStatsByRef(actor: ActorRef): Option[ChildRestartStats] =
    childrenRefs.get(actor.path.name).filter(_.child == actor)

  final def childByRef(actor: ActorRef): Option[ActorRef] =
    childStatsByRef(actor).map(_.child)

  protected def getAllChildStats: immutable.Iterable[ChildRestartStats] =
    childrenRefs.values.toList

  protected def removeChildAndGetStateChange(child: ActorRef): Option[SuspendReason] = {
    if (suspendReason ne null) {
      childrenRefs.remove(child.path.name)
      if (suspendReason == Termination && childrenRefs.isEmpty) {
        setTerminated()
        None
      } else {
        Some(suspendReason)
      }
    } else {
      childrenRefs.remove(child.path.name)
      None
    }
  }

  def actorOf(props: Props): ActorRef =
    makeChild(this, props, randomName(), async = false, systemService = false)
  def actorOf(props: Props, name: String): ActorRef =
    makeChild(this, props, checkName(name), async = false, systemService = false)

  private[actors] def attachChild(props: Props, systemService: Boolean): ActorRef =
    makeChild(this, props, randomName(), async = true, systemService = systemService)
  private[actors] def attachChild(props: Props, name: String, systemService: Boolean): ActorRef =
    makeChild(this, props, checkName(name), async = true, systemService = systemService)

  private[this] var _nextName: Long = _ // default = 0L
  final protected def randomName(): String = {
    val current = _nextName
    _nextName += 1
    Helpers.base64(current)
  }

  final def stop(actor: ActorRef): Unit = {
    if (isChild(actor)) {
      //this.shallDie += actor
      if (suspendReason eq null)
        suspendReason = UserRequest
    }
    actor.asInstanceOf[InternalActorRef].stop()
  }

  final def checkChildNameAvailable(name: String): Unit = {
    if (childrenRefs contains name)
      throw new InvalidActorNameException(s"actor name [$name] is not unique!")
  }

  final def isChild(ref: ActorRef): Boolean = {
    child(ref.path.name) == Some(ref)
  }

  final protected def setChildrenTerminationReason(reason: SuspendReason): Boolean = {
    if (suspendReason ne null) {
      suspendReason = reason
      true
    } else
      false
  }

  final protected def setTerminated(): Unit = {
    childrenRefs = sharedEmptyChildrenRefs
    suspendReason = null
    terminating = true
  }

  /*
   * ActorCell-internal API
   */

  protected def isNormal = !terminating && (suspendReason eq null)

  protected def isTerminating = terminating

  protected def waitingForChildrenOrNull = suspendReason match {
    case w: WaitingForChildren => w
    case _ => null
  }

  protected def suspendChildren(exceptFor: Set[ActorRef] = Set.empty): Unit = {
    childrenRefs.values foreach { stats =>
      if (!(exceptFor contains stats.child))
        (stats.child: InternalActorRef).suspend()
    }
  }

  protected def resumeChildren(causedByFailure: Throwable, perp: ActorRef): Unit = {
    childrenRefs.values foreach { stats =>
      (stats.child: InternalActorRef).resume(
          if (perp == stats.child) causedByFailure else null)
    }
  }

  /*
   * Private helpers
   */

  private def checkName(name: String): String = {
    name match {
      case null           => throw new InvalidActorNameException("actor name must not be null")
      case ""             => throw new InvalidActorNameException("actor name must not be empty")
      case ElementRegex() => name
      case _              => throw new InvalidActorNameException(s"illegal actor name [$name], must conform to $ElementRegex")
    }
  }

  private def makeChild(cell: ActorCell, props: Props, name: String,
      async: Boolean, systemService: Boolean): ActorRef = {

    /*
     * in case we are currently terminating, fail external attachChild requests
     * (internal calls cannot happen anyway because we are suspended)
     */
    if (isTerminating) {
      throw new IllegalStateException(
          "cannot create children while terminating or terminated")
    } else {
      checkChildNameAvailable(name)
      val childPath = new ChildActorPath(cell.self.path, name)(ActorCell.newUid())
      val child = new LocalActorRef(system, childPath, cell.self, props, dispatcher)
      // mailbox==null during RoutedActorCell constructor, where suspends are queued otherwise
      if (mailbox ne null) for (_ <- 1 to mailbox.suspendCount) child.suspend()
      if (childrenRefs eq sharedEmptyChildrenRefs)
        childrenRefs = JSMap.empty
      childrenRefs(name) = ChildRestartStats(child)
      child.start()
      child
    }

  }
}
