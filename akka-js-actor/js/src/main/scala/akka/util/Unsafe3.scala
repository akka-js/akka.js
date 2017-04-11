package akka.util

import akka.actor.dungeon.AbstractActorCell
import akka.actor.dungeon.ChildrenContainer.EmptyChildrenContainer
import akka.actor.FunctionRef
import scalajs.js
import scala.collection.mutable

object Unsafe /*_weak_map*/ {

    val unsafeVars: WeakMap[AnyRef, mutable.Map[Int, Any]] =
      new WeakMap[AnyRef, mutable.Map[Int, Any]]()

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
        if (unsafeVars.has(toAnyRef(o))) {
          val obj = unsafeVars.get(toAnyRef(o))

          obj.getOrElse(
            offset.asInstanceOf[Int],
            fallback(offset)
          ).asInstanceOf[AnyRef]
        } else fallback(offset).asInstanceOf[AnyRef]
      }

      def compareAndSwapObject(o: Any, offset: Long, old: Any, next: Any): Boolean = {
        if (unsafeVars.has(toAnyRef(o))) {
          val obj = unsafeVars.get(toAnyRef(o))

          val res = obj.get(offset.asInstanceOf[Int])

          if ((res.isEmpty && old == fallback(offset)) ||
              (res.isDefined && res.get == old)) {
            unsafeVars.set(toAnyRef(o),
              obj.updated(
                offset.asInstanceOf[Int],
                next
              )
            )
            true
          } else false
        } else {
          if (old == fallback(offset)) {
            unsafeVars.set(toAnyRef(o),
              mutable.Map[Int, Any](offset.asInstanceOf[Int] -> next)
            )
            true
          } else false
        }
      }

      def getAndSetObject(o: Any, offset: Long, next: Any): Any = {
        if (unsafeVars.has(toAnyRef(o))) {
          val obj = unsafeVars.get(toAnyRef(o))

          val res = obj(offset.asInstanceOf[Int])

          unsafeVars.set(toAnyRef(o),
            obj.updated(
              offset.asInstanceOf[Int],
              next
            )
          )

          res
        } else {
          unsafeVars.set(toAnyRef(o),
            mutable.Map[Int, Any](offset.asInstanceOf[Int] -> next)
          )

          fallback(offset)
        }
      }

      def getAndAddLong(o: Any, offset: Long, next: Long): Long = {
        if (unsafeVars.has(toAnyRef(o))) {
          val obj = unsafeVars.get(toAnyRef(o))

          val res = obj.get(offset.asInstanceOf[Int]).getOrElse(0L)

          unsafeVars.set(toAnyRef(o),
            obj.updated(
              offset.asInstanceOf[Int],
              res.asInstanceOf[Long] + next
            )
          )

          res.asInstanceOf[Long]
        } else {
          unsafeVars.set(toAnyRef(o),
            mutable.Map[Int, Any](offset.asInstanceOf[Int] -> next)
          )

          0L
        }
      }

      def getAndAddInt(o: Any, offset: Long, next: Int): Int = {
        if (unsafeVars.has(toAnyRef(o))) {
          val obj = unsafeVars.get(toAnyRef(o))

          val res = obj.get(offset.asInstanceOf[Int]).getOrElse(0)

          unsafeVars.set(toAnyRef(o),
            obj.updated(offset.asInstanceOf[Int],
              res.asInstanceOf[Int] + next
            )
          )

          res.asInstanceOf[Int]
        } else {
          unsafeVars.set(toAnyRef(o),
            mutable.Map[Int, Any](offset.asInstanceOf[Int] -> next)
          )

          0
        }
      }

      def putObjectVolatile(o: Any, offset: Long, next: Any): Unit = {
        if (unsafeVars.has(toAnyRef(o))) {
          unsafeVars.set(toAnyRef(o),
            unsafeVars.get(toAnyRef(o)).updated(
              offset.asInstanceOf[Int],
              next
            )
          )
        } else {
          unsafeVars.set(toAnyRef(o),
            mutable.Map[Int, Any](offset.asInstanceOf[Int] -> next)
          )
        }
      }
    }
}
