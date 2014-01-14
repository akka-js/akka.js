package org.scalajs.actors

import scala.scalajs.js

import dispatch._
import sysmsg.SystemMessage

private[actors] class LocalActorRef(
    system: ActorSystem,
    val path: ActorPath,
    _parent: ActorRef,
    _props: Props,
    _dispatcher: MessageDispatcher) extends InternalActorRef {

  val actorCell: ActorCell =
    new ActorCell(system, _props, _dispatcher, this, _parent)
  actorCell.init(sendSupervise = _parent ne null)
  actorCell.create()

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

  def getChild(name: Iterator[String]): InternalActorRef =
    Actor.noSender

  def isLocal: Boolean = true
}
