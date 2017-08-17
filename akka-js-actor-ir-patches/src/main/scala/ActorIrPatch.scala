package akka.actor

trait ActorRef {}

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

private[akka] class RepointableActorRef {

  var cellCallMeDirectly: Cell = _
  var lookupCallMeDirectly: Cell = _
}
