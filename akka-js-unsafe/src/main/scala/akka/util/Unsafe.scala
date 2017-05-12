package akka.util

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object Unsafe {

  def getObjectVolatileImpl(c: Context)(o: c.Expr[Any], offset: c.Expr[Int]): c.Expr[AnyRef] = {
    import c.universe._

    offset.tree match {
      case q"0" =>   //_cellDoNotCallMeDirectly
        c.Expr[AnyRef](q"""{
          type WithCell = {
            var cellCallMeDirectly: akka.actor.Cell
          }

          $o.asInstanceOf[WithCell].cellCallMeDirectly
        }""")
      case q"1" =>   //_lookupDoNotCallMeDirectly
        c.Expr[AnyRef](q"""{
          type WithLookup = {
            var lookupCallMeDirectly: akka.actor.Cell
          }

          $o.asInstanceOf[WithLookup].lookupCallMeDirectly
        }""")
      case q"AbstractActorCell.mailboxOffset" =>  //_mailboxDoNotCallMeDirectly
        c.Expr[AnyRef](q"""{
          type WithMailbox = {
            var mailboxCallMeDirectly: akka.dispatch.Mailbox
          }

          $o.asInstanceOf[WithMailbox].mailboxCallMeDirectly
        }""")
      case q"AbstractActorCell.childrenOffset" =>  //_childrenRefsDoNotCallMeDirectly
        c.Expr[AnyRef](q"""{
          type WithChildrenRefs = {
            var childrenRefsCallMeDirectly: akka.actor.dungeon.ChildrenContainer
          }

          val res = $o.asInstanceOf[WithChildrenRefs].childrenRefsCallMeDirectly

          if (res == null) EmptyChildrenContainer
          else res
        }""")
      case q"AbstractActorCell.functionRefsOffset" =>  //_functionRefsDoNotCallMeDirectly
        c.Expr[AnyRef](q"""{
          type WithFunctionRefs = {
            var functionRefsCallMeDirectly: Map[String, akka.actor.FunctionRef]
          }

          val res = $o.asInstanceOf[WithFunctionRefs].functionRefsCallMeDirectly

          if (res == null) Map.empty[String, akka.actor.FunctionRef]
          else res
        }""")
      case q"6" =>  //_currentStateDoNotCallMeDirectly
        c.Expr[AnyRef](q"""{
          type WithCurrentState = {
            var currentStateCallMeDirectly: AnyRef
          }

          val res = $o.asInstanceOf[WithCurrentState].currentStateCallMeDirectly

          if (res == null) Closed
          else res
        }""")
      case q"7" =>  //_currentResetTimeoutDoNotCallMeDirectly
        // not sure how to initialize this ...
        c.Expr[AnyRef](q"""{
          type WithCurrentResetTimeout = {
            var currentResetTimeoutCallMeDirectly: FiniteDuration
          }

          ???
        }""")
      case q"8" =>  //_stateDoNotCallMeDirectly
        c.Expr[AnyRef](q"""{
          type WithState = {
            var stateCallMeDirectly: AnyRef
          }

          $o.asInstanceOf[WithState].stateCallMeDirectly
        }""")
      case q"AbstractPromiseActorRef.watchedBy" | q"9" =>  //_watchedByDoNotCallMeDirectly
        c.Expr[AnyRef](q"""{
          type WithWatchedBy = {
            var watchedByCallMeDirectly: Set[ActorRef]
          }

          val ref = $o.asInstanceOf[WithWatchedBy].watchedByCallMeDirectly

          if (ref == null) Set[ActorRef]()
          else ref
        }""")
      case x =>
        c.error(c.enclosingPosition, s"This shouldn't happen ${offset.tree}")
        throw new Exception(s"Unmatched Unsafe usage at offset: $x")
    }
  }

  def compareAndSwapObjectImpl(c: Context)(o: c.Expr[Any], offset: c.Expr[Int], old: c.Expr[Any], next: c.Expr[Any]): c.Expr[Boolean] = {
    import c.universe._

    offset.tree match {
      case q"0" =>   //_cellDoNotCallMeDirectly
        c.Expr[Boolean](q"""{
          type WithCell = {
            var cellCallMeDirectly: akka.actor.Cell
          }

          if ($o.asInstanceOf[WithCell].cellCallMeDirectly == $old) {
            $o.asInstanceOf[WithCell].cellCallMeDirectly = $next
            true
          } else false
        }""")
      case q"1" =>   //_lookupDoNotCallMeDirectly
        c.Expr[Boolean](q"""{
          type WithLookup = {
            var lookupCallMeDirectly: akka.actor.Cell
          }

          if ($o.asInstanceOf[WithLookup].lookupCallMeDirectly == $old) {
            $o.asInstanceOf[WithLookup].lookupCallMeDirectly = $next
            true
          } else false
        }""")
      case q"AbstractActorCell.mailboxOffset" =>  //_mailboxDoNotCallMeDirectly
        c.Expr[Boolean](q"""{
          type WithMailbox = {
            var mailboxCallMeDirectly: akka.dispatch.Mailbox
          }

          if ($o.asInstanceOf[WithMailbox].mailboxCallMeDirectly == $old) {
            $o.asInstanceOf[WithMailbox].mailboxCallMeDirectly = $next
            true
          } else false
        }""")
      case q"AbstractActorCell.childrenOffset" =>  //_childrenRefsDoNotCallMeDirectly
        c.Expr[Boolean](q"""{
          type WithChildrenRefs = {
            var childrenRefsCallMeDirectly: akka.actor.dungeon.ChildrenContainer
          }

          if ($o.asInstanceOf[WithChildrenRefs].childrenRefsCallMeDirectly == $old ||
              ($old == EmptyChildrenContainer &&
              $o.asInstanceOf[WithChildrenRefs].childrenRefsCallMeDirectly == null)
            ) {
            $o.asInstanceOf[WithChildrenRefs].childrenRefsCallMeDirectly = $next
            true
          } else false
        }""")
      case q"AbstractActorCell.functionRefsOffset" =>  //_functionRefsDoNotCallMeDirectly
        c.Expr[Boolean](q"""{
          type WithFunctionRefs = {
            var functionRefsCallMeDirectly: Map[String, akka.actor.FunctionRef]
          }

          if ($o.asInstanceOf[WithFunctionRefs].functionRefsCallMeDirectly == $old ||
              ($o.asInstanceOf[WithFunctionRefs].functionRefsCallMeDirectly == null &&
              $old == Map.empty[String, akka.actor.FunctionRef]
              )
            ) {
            $o.asInstanceOf[WithFunctionRefs].functionRefsCallMeDirectly = $next
            true
          } else false
        }""")
      case q"6" =>  //_currentStateDoNotCallMeDirectly
        c.Expr[Boolean](q"""{
          type WithCurrentState = {
            var currentStateCallMeDirectly: Any
          }

          if ($o.asInstanceOf[WithCurrentState].currentStateCallMeDirectly == $old ||
              ($old == Closed &&
              $o.asInstanceOf[WithCurrentState].currentStateCallMeDirectly == null)
            ) {
            $o.asInstanceOf[WithCurrentState].currentStateCallMeDirectly = $next
            true
          } else false
        }""")
      case q"7" =>  //_currentResetTimeoutDoNotCallMeDirectly
        // not sure how to initialize this ...
        c.Expr[Boolean](q"""{
          type WithCurrentResetTimeout = {
            var currentResetTimeoutCallMeDirectly: FiniteDuration
          }

          ???
        }""")
      case q"8" =>  //_stateDoNotCallMeDirectly
        c.Expr[Boolean](q"""{
          type WithState = {
            var stateCallMeDirectly: AnyRef
          }

          if ($o.asInstanceOf[WithState].stateCallMeDirectly == $old) {
            $o.asInstanceOf[WithState].stateCallMeDirectly = $next
            true
          } else false
        }""")
      case q"AbstractPromiseActorRef.watchedBy" | q"9" =>  //_watchedByDoNotCallMeDirectly
        c.Expr[Boolean](q"""{
          type WithWatchedBy = {
            var watchedByCallMeDirectly: Set[ActorRef]
          }

          if ($o.asInstanceOf[WithWatchedBy].watchedByCallMeDirectly == $old ||
              ($old == Set[ActorRef]() &&
              $o.asInstanceOf[WithWatchedBy].watchedByCallMeDirectly == null)
            ) {
            $o.asInstanceOf[WithWatchedBy].watchedByCallMeDirectly = $next
            true
          } else false
        }""")
      case x =>
        c.error(c.enclosingPosition, s"This shouldn't happen ${offset.tree}")
        throw new Exception(s"Unmatched Unsafe usage at offset: $x")
    }
  }

  def putObjectVolatileImpl(c: Context)(o: c.Expr[Any], offset: c.Expr[Int], next: c.Expr[Any]): c.Expr[Unit] = {
    import c.universe._

    offset.tree match {
      case q"AbstractActorCell.childrenOffset" =>  //_childrenRefsDoNotCallMeDirectly
        c.Expr[Unit](q"""{
          type WithChildrenRefs = {
            var childrenRefsCallMeDirectly: akka.actor.dungeon.ChildrenContainer
          }

          $o.asInstanceOf[WithChildrenRefs].childrenRefsCallMeDirectly = $next
        }""")
      case q"8" =>  //_stateDoNotCallMeDirectly
        c.Expr[Unit](q"""{
          type WithState = {
            var stateCallMeDirectly: AnyRef
          }

          $o.asInstanceOf[WithState].stateCallMeDirectly = $next
        }""")
      case x =>
        c.error(c.enclosingPosition, s"This shouldn't happen ${offset.tree}")
        throw new Exception(s"Unmatched Unsafe usage at offset: $x")
    }
  }

  def getAndSetObjectImpl(c: Context)(o: c.Expr[Any], offset: c.Expr[Int], next: c.Expr[Any]): c.Expr[Any] = {
    import c.universe._

    offset.tree match {
      case q"AbstractActorCell.functionRefsOffset" =>  //_functionRefsDoNotCallMeDirectly
        c.Expr[AnyRef](q"""{
          type WithFunctionRefs = {
            var functionRefsCallMeDirectly: Map[String, akka.actor.FunctionRef]
          }

          val res = $o.asInstanceOf[WithFunctionRefs].functionRefsCallMeDirectly

          $o.asInstanceOf[WithFunctionRefs].functionRefsCallMeDirectly = $next.asInstanceOf[Map[String, akka.actor.FunctionRef]]

          if (res == null) Map.empty[String, akka.actor.FunctionRef]
          else res
        }""")
      case x =>
        c.error(c.enclosingPosition, s"This shouldn't happen ${offset.tree}")
        throw new Exception(s"Unmatched Unsafe usage at offset: $x")
    }
  }

  def getAndAddLongImpl(c: Context)(o: c.Expr[Any], offset: c.Expr[Int], next: c.Expr[Long]): c.Expr[Long] = {
    import c.universe._

    offset.tree match {
      case q"AbstractActorCell.nextNameOffset" =>   //_nextNameDoNotCallMeDirectly
        c.Expr[Long](q"""{
          type WithNextName = {
            var nextNameCallMeDirectly: Long
          }

          val res = $o.asInstanceOf[WithNextName].nextNameCallMeDirectly

          $o.asInstanceOf[WithNextName].nextNameCallMeDirectly += $next

          res
        }""")
      case x =>
        c.error(c.enclosingPosition, s"This shouldn't happen ${offset.tree}")
        throw new Exception(s"Unmatched Unsafe usage at offset: $x")
    }
  }

  object Instance {

    def getObjectVolatile(o: Any, offset: Int): AnyRef =
      macro getObjectVolatileImpl

    def compareAndSwapObject(o: Any, offset: Int, old: Any, next: Any): Boolean =
      macro compareAndSwapObjectImpl

    def putObjectVolatile(o: Any, offset: Int, next: Any): Unit =
      macro putObjectVolatileImpl

    def getAndSetObject(o: Any, offset: Int, next: Any): Any =
      macro getAndSetObjectImpl

    def getAndAddLong(o: Any, offset: Int, next: Long): Long =
      macro getAndAddLongImpl

  }

  final val instance = Instance

}
