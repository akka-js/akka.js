package akka.actor
package dungeon

trait ChildrenContainer {}

private[akka] trait Children {
  var mailboxCallMeDirectly: akka.dispatch.Mailbox = _
  var childrenRefsCallMeDirectly: akka.actor.dungeon.ChildrenContainer = _
}
