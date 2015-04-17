package akka.actor

import akka.util.JSMap

/**
 * Internal implementation detail used for paths like “/temp”
 *
 * INTERNAL API
 */
private[akka] class VirtualPathContainer(
    override val provider: ActorRefProvider,
    override val path: ActorPath,
    override val getParent: InternalActorRef/*,
    val log: LoggingAdapter*/) extends MinimalActorRef {

  private val children = JSMap.empty[InternalActorRef]

  def addChild(name: String, ref: InternalActorRef): Unit = {
    children.put(name, ref) match {
      case null ⇒ // okay
      case old ⇒
        // this can happen from RemoteSystemDaemon if a new child is created
        // before the old is removed from RemoteSystemDaemon children
        //log.debug("{} replacing child {} ({} -> {})", path, name, old, ref)
    }
  }

  def removeChild(name: String): Unit =
    if (children.remove(name) eq null)
      () //log.warning("{} trying to remove non-child {}", path, name)

  /**
   * Remove a named child if it matches the ref.
   */
  protected def removeChild(name: String, ref: ActorRef): Unit = {
    val current = getChild(name)
    if (current eq null)
      ()//log.warning("{} trying to remove non-child {}", path, name)
    else if (current == ref)
      children.remove(name)
  }

  def getChild(name: String): InternalActorRef = children(name)

  override def getChild(name: Iterator[String]): InternalActorRef = {
    if (name.isEmpty) this
    else {
      val n = name.next()
      if (n.isEmpty) this
      else children.get(n) match {
        case None => Nobody
        case Some(some) =>
          if (name.isEmpty) some
          else some.getChild(name)
      }
    }
  }

  def hasChildren: Boolean = !children.isEmpty

  def foreachChild(f: ActorRef => Unit): Unit = {
    val iter = children.values.iterator
    while (iter.hasNext) f(iter.next)
  }
}
