package org.scalajs.actors

abstract class ActorSystem(val name: String) {
  def actorOf(props: Props): ActorRef = actorOf(props, "")
  def actorOf(props: Props, name: String): ActorRef

  def deadLetters: ActorRef
}

object ActorSystem {
  def apply(name: String): ActorSystem =
    new ActorSystemImpl(name)
}

class ActorSystemImpl(nme: String) extends ActorSystem(nme) {
  def actorOf(props: Props, name: String): ActorRef = {
    new LocalActorRef(this, props)
  }

  val deadLetters: ActorRef = new DeadLettersActorRef(this)
}
