package akka.util

import scala.collection.mutable

import akka.actor.dungeon.AbstractActorCell
import akka.actor.dungeon.ChildrenContainer.EmptyChildrenContainer
import akka.actor.FunctionRef
import scalajs.js

object Unsafe_weak_dynamic {

    val unsafeVars: WeakMap[AnyRef, js.Dynamic] =
      new WeakMap[AnyRef, js.Dynamic]()

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
        val obj = unsafeVars.get(toAnyRef(o))

        if (js.isUndefined(obj))
          fallback(offset).asInstanceOf[AnyRef]
        else {
          val res = obj.selectDynamic(offset.toString)

          if (js.isUndefined(res))
            fallback(offset).asInstanceOf[AnyRef]
          else res.asInstanceOf[AnyRef]
        }
      }

      def compareAndSwapObject(o: Any, offset: Long, old: Any, next: Any) = {
        val obj = unsafeVars.get(toAnyRef(o))

        if (js.isUndefined(obj))
          unsafeVars.set(toAnyRef(o),
            js.Dynamic.literal(offset.toString -> next.asInstanceOf[js.Any])
          )
        else {
          if (next == null)
            obj.deleteDynamic(offset.toString)
          else
            obj.updateDynamic(offset.toString)(next.asInstanceOf[js.Any])
        }

        true
      }

      def getAndSetObject(o: Any, offset: Long, next: Any) = {
        val obj = unsafeVars.get(toAnyRef(o))
        if (js.isUndefined(obj)) {
          unsafeVars.set(toAnyRef(o),
            js.Dynamic.literal(offset.toString -> next.asInstanceOf[js.Any])
          )

          null
        } else {
          val old = obj(offset.toString)

          if (next == null)
            obj.deleteDynamic(offset.toString)
          else
            obj.updateDynamic(offset.toString)(next.asInstanceOf[js.Any])

          if (js.isUndefined(old)) null
          else old
        }

      }

      def getAndAddLong(o: Any, offset: Long, next: Long) = {
        val obj = unsafeVars.get(toAnyRef(o))
        if (js.isUndefined(obj)) {
          unsafeVars.set(toAnyRef(o),
            js.Dynamic.literal(offset.toString -> next)
          )

          0L
        } else {
          val old = obj.selectDynamic(offset.toString)

          if (js.isUndefined(old))
            obj.updateDynamic(offset.toString)(
                next.asInstanceOf[js.Any])
          else
            obj.updateDynamic(offset.toString)(
                (old.asInstanceOf[Long] + next).asInstanceOf[js.Any])

          if (js.isUndefined(old)) 0L
          else old.asInstanceOf[Long]
        }
      }

      def getAndAddInt(o: Any, offset: Long, next: Int) = {
        val obj = unsafeVars.get(toAnyRef(o))
        if (js.isUndefined(obj)) {
          unsafeVars.set(toAnyRef(o),
            js.Dynamic.literal(offset.toString -> next)
          )

          0
        } else {
          val old = obj.selectDynamic(offset.toString)

          if (js.isUndefined(old))
            obj.updateDynamic(offset.toString)(
                next.asInstanceOf[js.Any])
          else
            obj.updateDynamic(offset.toString)(
                (old.asInstanceOf[Int] + next).asInstanceOf[js.Any])

          if (js.isUndefined(old)) 0
          else old.asInstanceOf[Int]
        }
      }

      def putObjectVolatile(o: Any, offset: Long, next: Any) = {
        val obj = unsafeVars.get(toAnyRef(o))
        if (js.isUndefined(obj)) {
          unsafeVars.set(toAnyRef(o),
            js.Dynamic.literal(offset.toString -> next.asInstanceOf[js.Any])
          )

          null
        } else obj.updateDynamic(offset.toString)(next.asInstanceOf[js.Any])
      }

    }
}
