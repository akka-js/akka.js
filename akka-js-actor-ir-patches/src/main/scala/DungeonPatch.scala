package akka.actor
package dungeon

trait ChildrenContainer {}

private[akka] trait Children {
  var childrenRefsCallMeDirectly: akka.actor.dungeon.ChildrenContainer = _
  var nextNameCallMeDirectly: Long = _
  var functionRefsCallMeDirectly: Map[String, akka.actor.FunctionRef] = _
}

private[akka] trait Dispatch {
  var mailboxCallMeDirectly: akka.dispatch.Mailbox = _
}
