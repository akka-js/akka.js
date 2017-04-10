package akka.util

import akka.actor.dungeon.AbstractActorCell
import akka.actor.dungeon.ChildrenContainer.EmptyChildrenContainer
import akka.actor.FunctionRef
import scalajs.js

object Unsafe /*_weak_dictionary*/ {

    val unsafeVars: WeakMap[AnyRef, js.Dictionary[Any]] =
      new WeakMap[AnyRef, js.Dictionary[Any]]()

    def fallback(offset: Long) = {
      //Missing initializations...
      if (offset == AbstractActorCell.childrenOffset)
        EmptyChildrenContainer
      else if (offset == AbstractActorCell.nextNameOffset)
        0L
      else if (offset == AbstractActorCell.functionRefsOffset)
        Map.empty[String, FunctionRef]
      else null
    }

    def toAnyRef(a: Any): AnyRef =
      a.asInstanceOf[AnyRef]

    final val instance = new {

      def getObjectVolatile(o: Any, offset: Long): AnyRef = {
        println("6")
        if (unsafeVars.has(toAnyRef(o))) {
          val obj = unsafeVars.get(toAnyRef(o))

          obj.getOrElse(
            offset.toString,
            fallback(offset)
          ).asInstanceOf[AnyRef]
        } else fallback(offset).asInstanceOf[AnyRef]
      }

      def compareAndSwapObject(o: Any, offset: Long, old: Any, next: Any): Boolean = {
        println("5")
        if (unsafeVars.has(toAnyRef(o))) {
          val obj = unsafeVars.get(toAnyRef(o))

          val res = obj.get(offset.toString)

          if ((res.isEmpty && old == fallback(offset)) ||
              (res.isDefined && res.get == old)) {

            unsafeVars.set(toAnyRef(o),
              obj.updated(
                offset.toString,
                next
              ).asInstanceOf[js.Dictionary[Any]]
            )
            true
          } else false
        } else {
          if (old == fallback(offset)) {
            unsafeVars.set(toAnyRef(o),
              js.Dictionary(offset.toString -> next)
            )
            true
          } else false
        }
      }

      def getAndSetObject(o: Any, offset: Long, next: Any): Any = {
        println("4")
        if (unsafeVars.has(toAnyRef(o))) {
          val obj = unsafeVars.get(toAnyRef(o))

          val res = obj(offset.toString)

          unsafeVars.set(toAnyRef(o),
            obj.updated(
              offset.toString,
              next
            ).asInstanceOf[js.Dictionary[Any]]
          )

          res
        } else {
          unsafeVars.set(toAnyRef(o),
            js.Dictionary(offset.toString -> next)
          )

          fallback(offset)
        }
      }

      def getAndAddLong(o: Any, offset: Long, next: Long): Long = {
        println("3")
        if (unsafeVars.has(toAnyRef(o))) {
          val obj = unsafeVars.get(toAnyRef(o))

          val res = obj(offset.toString)

          unsafeVars.set(toAnyRef(o),
            res.updated(
              offset.toString,
              res.asInstanceOf[Long] + next
            ).asInstanceOf[js.Dictionary[Any]]
          )

          res.asInstanceOf[Long]
        } else {
          unsafeVars.set(
            toAnyRef(o),
            js.Dictionary[Any](offset.toString -> next)
          )

          0L
        }
      }

      def getAndAddInt(o: Any, offset: Long, next: Int): Int = {
        println("2")
        if (unsafeVars.has(toAnyRef(o))) {
          val obj = unsafeVars.get(toAnyRef(o))

          val res = obj(offset.toString)

          unsafeVars.set(toAnyRef(o),
            res.updated(offset.toString,
              res.asInstanceOf[Int] + next
            ).asInstanceOf[js.Dictionary[Any]]
          )

          res.asInstanceOf[Int]
        } else {
          unsafeVars.set(toAnyRef(o),
            js.Dictionary[Any](offset.toString -> next)
          )

          0
        }
      }

      def putObjectVolatile(o: Any, offset: Long, next: Any): Unit = {
        println("1")
        if (unsafeVars.has(toAnyRef(o))) {
          unsafeVars.set(toAnyRef(o),
            unsafeVars.get(toAnyRef(o)).updated(
              offset.toString,
              next
            ).asInstanceOf[js.Dictionary[Any]]
          )
        } else {
          unsafeVars.set(
            toAnyRef(o),
            js.Dictionary(offset.toString -> next)
          )
        }
      }
    }
}
