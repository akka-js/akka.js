package org.scalajs.actors

class DeadLettersActorRef(system: ActorSystem) extends MinimalActorRef {
  def path: ActorPath = ???

  override def !(msg: Any)(implicit sender: ActorRef): Unit = {
    // TODO, ignore for now
  }
}
