package org.scalajs.actors
package cell

import scala.annotation.tailrec
import scala.collection.{ immutable, mutable }

import org.scalajs.actors.util.Helpers

import ActorPath.ElementRegex

private[actors] trait Children { this: ActorCell =>

  import ChildrenContainer._

  private var childrenRefs: ChildrenContainer = EmptyChildrenContainer

  final def children: immutable.Iterable[ActorRef] =
    childrenRefs.children

  final def childStatsByName(name: String): Option[ChildRestartStats] =
    childrenRefs.getByName(name)

  final def child(name: String): Option[ActorRef] =
    childStatsByName(name).map(_.child)

  final def childStatsByRef(actor: ActorRef): Option[ChildRestartStats] =
    childrenRefs.getByRef(actor)

  final def childByRef(actor: ActorRef): Option[ActorRef] =
    childStatsByRef(actor).map(_.child)

  final def isChild(actor: ActorRef): Boolean =
    childrenRefs.isChild(actor)

  protected def getAllChildStats: immutable.Iterable[ChildRestartStats] =
    childrenRefs.stats

  def actorOf(props: Props): ActorRef =
    makeChild(this, props, randomName(), async = false, systemService = false)
  def actorOf(props: Props, name: String): ActorRef =
    makeChild(this, props, checkName(name), async = false, systemService = false)

  private[actors] def attachChild(props: Props, systemService: Boolean): ActorRef =
    makeChild(this, props, randomName(), async = true, systemService = systemService)
  private[actors] def attachChild(props: Props, name: String, systemService: Boolean): ActorRef =
    makeChild(this, props, checkName(name), async = true, systemService = systemService)

  private var _nextName: Long = _ // default = 0L
  final protected def randomName(): String = {
    val current = _nextName
    _nextName += 1
    Helpers.base64(current)
  }

  final def stop(actor: ActorRef): Unit = {
    if (childrenRefs.isChild(actor)) {
      childrenRefs = childrenRefs.shallDie(actor)
    }
    actor.asInstanceOf[InternalActorRef].stop()
  }

  final def checkChildNameAvailable(name: String): Unit = {
    if (childrenRefs.isChild(name))
      throw new InvalidActorNameException(s"actor name [$name] is not unique!")
  }

  final protected def setChildrenTerminationReason(
      reason: ChildrenContainer.SuspendReason): Boolean = {
    childrenRefs match {
      case c: ChildrenContainer.TerminatingChildrenContainer =>
        childrenRefs = c.copy(reason = reason)
        true
      case _ => false
    }
  }

  final protected def setTerminated(): Unit =
    childrenRefs = TerminatedChildrenContainer

  /*
   * ActorCell-internal API
   */

  protected def isNormal = childrenRefs.isNormal

  protected def isTerminating = childrenRefs.isTerminating

  private[actors] def initChild(child: ActorRef): Unit =
    childrenRefs = childrenRefs.add(child.path.name, ChildRestartStats(child))

  protected def waitingForChildrenOrNull = childrenRefs match {
    case TerminatingChildrenContainer(_, _, w: WaitingForChildren) => w
    case _ => null
  }

  protected def suspendChildren(exceptFor: Set[ActorRef] = Set.empty): Unit =
    childrenRefs.stats foreach {
      case ChildRestartStats(child, _, _) if !(exceptFor contains child) =>
        child.asInstanceOf[InternalActorRef].suspend()
      case _ =>
    }

  protected def resumeChildren(causedByFailure: Throwable, perp: ActorRef): Unit =
    childrenRefs.stats foreach {
      case ChildRestartStats(child, _, _) =>
        child.asInstanceOf[InternalActorRef].resume(
            if (perp == child) causedByFailure else null)
    }

  protected def removeChildAndGetStateChange(child: ActorRef): Option[SuspendReason] = {
    childrenRefs match { // The match must be performed BEFORE the removeChild
      case TerminatingChildrenContainer(_, _, reason) =>
        childrenRefs = childrenRefs.remove(child)
        childrenRefs match {
          case _: TerminatingChildrenContainer => None
          case _                               => Some(reason)
        }
      case _ =>
        childrenRefs = childrenRefs.remove(child)
        None
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
      val child = cell.provider.actorOf(cell.systemImpl, props, cell.self,
          childPath, systemService = systemService, async = async)
      // mailbox==null during RoutedActorCell constructor, where suspends are queued otherwise
      if (mailbox ne null) for (_ <- 1 to mailbox.suspendCount) child.suspend()
      initChild(child)
      child.start()
      child
    }

  }
}
