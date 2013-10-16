package ch.epfl.lamp.scalajs.actors

class DeadLettersActorRef(system: ActorSystem) extends ActorRef {
  def !(msg: Any)(implicit sender: ActorRef): Unit = {
    // TODO, ignore for now
  }
}
