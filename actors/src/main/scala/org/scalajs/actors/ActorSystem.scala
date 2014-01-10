package org.scalajs.actors

abstract class ActorSystem(val name: String) extends ActorRefFactory {
  def deadLetters: ActorRef
}

object ActorSystem {
  def apply(name: String): ActorSystem =
    new ActorSystemImpl(name)
}

class ActorSystemImpl(nme: String) extends ActorSystem(nme)
                                      with webworkers.WebWorkersActorSystem {
  def actorOf(props: Props): ActorRef =
    guardian.actorCell.actorOf(props)
  def actorOf(props: Props, name: String): ActorRef =
    guardian.actorCell.actorOf(props, name)

  def stop(ref: ActorRef): Unit = ???

  val guardian = new LocalActorRef(this, RootActorPath(Address(name)), null,
      Props(new Guardian))
  val deadLetters: ActorRef = new DeadLettersActorRef(this)
}
