package org.scalajs.actors

import dispatch._

abstract class ActorSystem(val name: String) extends ActorRefFactory {
  def guardian: ActorRef
  def deadLetters: ActorRef

  def sendToPath(path: ActorPath, message: Any)(
      implicit sender: ActorRef = Actor.noSender): Unit
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

  val deadLetters: ActorRef = new DeadLettersActorRef(this)

  val mailboxes: Mailboxes = new Mailboxes(deadLetters)
  val dispatcher: MessageDispatcher = new MessageDispatcher(mailboxes)

  val guardian = new LocalActorRef(this, RootActorPath(Address(name)), null,
      Props(new Guardian), dispatcher)

  override def sendToPath(path: ActorPath, message: Any)(
      implicit sender: ActorRef): Unit = {
    // FIXME The existence of this method is a hack! Need to find a solution.
    new webworkers.WorkerActorRef(this, path) ! message
  }

  def resolveLocalActorPath(path: ActorPath): Option[ActorRef] = {
    println(path.elements)
    val result = path.elements.foldLeft(guardian) { (parent, childName) =>
      parent.actorCell.child(childName) match {
        case Some(child: LocalActorRef) => child
        case x =>
          println(s"$parent of name ${parent.path}.child($childName) = $x")
          return None
      }
    }
    Some(result)
  }
}
