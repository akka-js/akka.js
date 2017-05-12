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

trait FunctionRef {}

trait Cell {}

private[akka] class ActorCell {
  final var props: Props = null

  var mailboxCallMeDirectly: akka.dispatch.Mailbox = _
  var childrenRefsCallMeDirectly: akka.actor.dungeon.ChildrenContainer = _
  var nextNameCallMeDirectly: Long = _
  var functionRefsCallMeDirectly: Map[String, akka.actor.FunctionRef] = _
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
