package akka.actor

trait ActorRef {}

trait ActorContext {
  def self: ActorRef
}

trait Actor {
  implicit var context: ActorContext = null
  implicit final var self: ActorRef = null
}

trait Props {}

trait FunctionRef {}

trait Cell {}

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

private[akka] class RepointableActorRef {

  var cellCallMeDirectly: Cell = _
  var lookupCallMeDirectly: Cell = _
}
