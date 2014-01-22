/**
 *  Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.actor
package dungeon

import scala.collection.immutable

import akka.util.JSMap
import akka.util.Collections.EmptyImmutableSeq

/**
 * INTERNAL API
 *
 * Unlike in Akka/JVM, instances of ChildrenContainer are <em>mutable</em>.
 * They will sometimes return themselves if possible to avoid unnecessary
 * allocations. Moreover, even when returning a different instance, the
 * internal states of the old and new instances may share mutable data. An
 * instance of ChildrenContrainer is therefore <em>invalid</em> after calling
 * any state transforming method on it (i.e., one of add(), remove() and
 * shallDie()).
 *
 * The only subclasses that are really immutable, and hence can be shared by
 * different [[org.scalajs.actors.cell.Children]], are the singletons
 * EmptyChildrenContainer and TerminatedChildrenContainer.
 */
private[akka] abstract class ChildrenContainer {

  def add(name: String, stats: ChildRestartStats): ChildrenContainer
  def remove(child: ActorRef): ChildrenContainer

  def getByName(name: String): Option[ChildRestartStats]
  def getByRef(actor: ActorRef): Option[ChildRestartStats]

  def isChild(name: String): Boolean = getByName(name).isDefined
  def isChild(actor: ActorRef): Boolean = getByRef(actor).isDefined

  def children: immutable.Iterable[ActorRef]
  def stats: immutable.Iterable[ChildRestartStats]

  def shallDie(actor: ActorRef): ChildrenContainer

  def isTerminating: Boolean = false
  def isNormal: Boolean = true
}

/**
 * INTERNAL API
 *
 * This object holds the classes performing the logic of managing the children
 * of an actor, hence they are intimately tied to ActorCell.
 */
private[akka] object ChildrenContainer {

  sealed trait SuspendReason
  case object UserRequest extends SuspendReason
  // careful with those system messages, all handling to be taking place in ActorCell.scala!
  case class Recreation(cause: Throwable) extends SuspendReason with WaitingForChildren
  case class Creation() extends SuspendReason with WaitingForChildren
  case object Termination extends SuspendReason

  trait WaitingForChildren

  abstract class EmptyChildrenContainer extends ChildrenContainer {
    override def add(name: String, stats: ChildRestartStats): ChildrenContainer =
      new NormalChildrenContainer((JSMap.empty[ChildRestartStats]) += name -> stats)
    override def remove(child: ActorRef): ChildrenContainer = this
    override def getByName(name: String): Option[ChildRestartStats] = None
    override def getByRef(actor: ActorRef): Option[ChildRestartStats] = None
    override def children: immutable.Iterable[ActorRef] = EmptyImmutableSeq
    override def stats: immutable.Iterable[ChildRestartStats] = EmptyImmutableSeq
    override def shallDie(actor: ActorRef): ChildrenContainer = this
  }

  /**
   * This is the empty container, shared among all leaf actors.
   */
  object EmptyChildrenContainer extends EmptyChildrenContainer {
    override def toString = "no children"
  }

  /**
   * This is the empty container which is installed after the last child has
   * terminated while stopping; it is necessary to distinguish from the normal
   * empty state while calling handleChildTerminated() for the last time.
   */
  object TerminatedChildrenContainer extends EmptyChildrenContainer {
    override def add(name: String, stats: ChildRestartStats): ChildrenContainer = this
    override def isTerminating: Boolean = true
    override def isNormal: Boolean = false
    override def toString = "terminated"
  }

  /**
   * Normal children container: we do have at least one child, but none of our
   * children are currently terminating (which is the time period between
   * calling context.stop(child) and processing the ChildTerminated() system
   * message).
   */
  class NormalChildrenContainer(
      c: JSMap[ChildRestartStats]) extends ChildrenContainer {

    override def add(name: String, stats: ChildRestartStats): ChildrenContainer = {
      c(name) = stats
      this
    }

    override def remove(child: ActorRef): ChildrenContainer = {
      c -= child.path.name
      if (c.isEmpty) EmptyChildrenContainer
      else this
    }

    override def getByName(name: String): Option[ChildRestartStats] =
      c.get(name)

    override def getByRef(actor: ActorRef): Option[ChildRestartStats] =
      c.get(actor.path.name).filter(_.child == actor)

    override def children: immutable.Iterable[ActorRef] =
      c.values.map(_.child).toList

    override def stats: immutable.Iterable[ChildRestartStats] =
      c.values.toList

    override def shallDie(actor: ActorRef): ChildrenContainer =
      TerminatingChildrenContainer(c, Set(actor), UserRequest)

    override def toString =
      if (c.size > 20) c.size + " children"
      else c.mkString("children:\n    ", "\n    ", "")
  }

  /**
   * Waiting state: there are outstanding termination requests (i.e.,
   * context.stop(child) was called but the corresponding ChildTerminated()
   * system message has not yet been processed). There could be no specific
   * reason (UserRequested), we could be Restarting or Terminating.
   *
   * Removing the last child which was supposed to be terminating will return
   * a different type of container, depending on whether or not children are
   * left and whether or not the reason was “Terminating”.
   */
  case class TerminatingChildrenContainer(
      c: JSMap[ChildRestartStats], toDie: Set[ActorRef], reason: SuspendReason)
      extends ChildrenContainer {
    // TODO Test whether we can also turn toDie into a mutable set

    override def add(name: String, stats: ChildRestartStats): ChildrenContainer = {
      c(name) = stats
      this
    }

    override def remove(child: ActorRef): ChildrenContainer = {
      val t = toDie - child
      if (t.isEmpty) {
        reason match {
          case Termination => TerminatedChildrenContainer
          case _           =>
            c -= child.path.name
            if (c.isEmpty) EmptyChildrenContainer
            else new NormalChildrenContainer(c)
        }
      } else {
        c -= child.path.name
        copy(toDie = t)
      }
    }

    override def getByName(name: String): Option[ChildRestartStats] =
      c.get(name)

    override def getByRef(actor: ActorRef): Option[ChildRestartStats] =
      c.get(actor.path.name).filter(_.child == actor)

    override def children: immutable.Iterable[ActorRef] =
      c.values.map(_.child).toList

    override def stats: immutable.Iterable[ChildRestartStats] =
      c.values.toList

    override def shallDie(actor: ActorRef): ChildrenContainer =
      copy(toDie = toDie + actor)

    override def isTerminating: Boolean = reason == Termination
    override def isNormal: Boolean = reason == UserRequest

    override def toString =
      if (c.size > 20) c.size + " children"
      else c.mkString("children (" + toDie.size + " terminating):\n    ", "\n    ", "\n") + toDie
  }

}
