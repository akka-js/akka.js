package akka.actor

import scala.scalajs.js

import akka.dispatch._
import akka.dispatch.sysmsg.SystemMessage

private[akka] class LocalActorRef(
    system: ActorSystem,
    val path: ActorPath,
    _parent: ActorRef,
    _props: Props,
    _dispatcher: MessageDispatcher) extends InternalActorRef {

  assert(path.uid != ActorCell.undefinedUid || path.isInstanceOf[RootActorPath],
      s"Trying to create a LocalActorRef with uid-less path $path")

  val actorCell: ActorCell =
    new ActorCell(system, _props, _dispatcher, this, _parent)
  actorCell.init(sendSupervise = _parent ne null)

  private[akka] def underlying = actorCell

  def !(msg: Any)(implicit sender: ActorRef): Unit =
    actorCell.sendMessage(Envelope(msg, sender, system))

  // InternalActorRef API

  def start(): Unit = actorCell.start()
  def resume(causedByFailure: Throwable): Unit = actorCell.resume(causedByFailure)
  def suspend(): Unit = actorCell.suspend()
  def restart(cause: Throwable): Unit = actorCell.restart(cause)
  def stop(): Unit = actorCell.stop()
  def sendSystemMessage(message: SystemMessage): Unit = actorCell.sendSystemMessage(message)

  def getParent: InternalActorRef = _parent

  override def provider: ActorRefProvider = actorCell.provider

  /**
   * Method for looking up a single child beneath this actor. Override in order
   * to inject “synthetic” actor paths like “/temp”.
   * It is racy if called from the outside.
   */
  def getSingleChild(name: String): InternalActorRef = {
    val (childName, uid) = ActorCell.splitNameAndUid(name)
    actorCell.childStatsByName(childName) match {
      case Some(crs: ChildRestartStats) if uid == ActorCell.undefinedUid || uid == crs.uid =>
        crs.child.asInstanceOf[InternalActorRef]
      case _ => Nobody
    }
  }

  def getChild(name: Iterator[String]): InternalActorRef =
    Actor.noSender

  def isLocal: Boolean = true
}
