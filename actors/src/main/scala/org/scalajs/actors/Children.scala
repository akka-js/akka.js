package org.scalajs.actors

import scala.annotation.tailrec
import scala.collection.{ immutable, mutable }

import org.scalajs.actors.util.Helpers

import ActorPath.ElementRegex

private[actors] trait Children { this: ActorCell =>
  private[this] val childrenRefs: JSMap[ActorRef] = JSMap.empty

  final def children: immutable.Iterable[ActorRef] =
    childrenRefs.values.toList

  final def child(name: String): Option[ActorRef] =
    childrenRefs.get(name)

  def actorOf(props: Props): ActorRef =
    makeChild(this, props, randomName(), async = false, systemService = false)
  def actorOf(props: Props, name: String): ActorRef =
    makeChild(this, props, checkName(name), async = false, systemService = false)

  private[actors] def attachChild(props: Props, systemService: Boolean): ActorRef =
    makeChild(this, props, randomName(), async = true, systemService = systemService)
  private[actors] def attachChild(props: Props, name: String, systemService: Boolean): ActorRef =
    makeChild(this, props, checkName(name), async = true, systemService = systemService)

  private[this] var _nextName = 0L
  final protected def randomName(): String = {
    val current = _nextName
    _nextName += 1
    Helpers.base64(current)
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

    val childPath = new ChildActorPath(cell.self.path, name)
    val child = new LocalActorRef(system, childPath, cell.self, props, dispatcher)
    childrenRefs(name) = child
    child
  }
}
