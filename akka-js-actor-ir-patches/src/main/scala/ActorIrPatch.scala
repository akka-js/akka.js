package akka.actor

trait ActorRef {}

trait ActorContext {
  def self: ActorRef
}

class Actor {
  implicit var context: ActorContext = null
  implicit final var self: ActorRef = null
}

trait Props {}

private[akka] class ActorCell {
  final var props: Props = null
}

private[akka] object LocalActorRefProvider {

  private class Guardian {
    implicit var context: ActorContext = null
    implicit final var self: ActorRef = null
  }

  private class SystemGuardian {
    implicit var context: ActorContext = null
    implicit final var self: ActorRef = null
  }
}
