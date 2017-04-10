package akka.util

import akka.actor.dungeon.AbstractActorCell
import akka.actor.dungeon.ChildrenContainer.EmptyChildrenContainer
import akka.actor.FunctionRef
import scalajs.js

object Unsafe_array_weak {

    val unsafeVarsArray: Array[WeakMap[AnyRef, Any]] =
      (for(_ <- 0 until 100) yield {new WeakMap[AnyRef, Any]()})
      .toArray

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
        val key = offset.asInstanceOf[Int]
        val res = unsafeVarsArray(key).get(toAnyRef(o))
        if (js.isUndefined(res))
          fallback(offset).asInstanceOf[AnyRef]
        else
          res.asInstanceOf[AnyRef]
      }

      def compareAndSwapObject(o: Any, offset: Long, old: Any, next: Any) = {
        val key = offset.asInstanceOf[Int]
        if (next == null)
          unsafeVarsArray(key).delete(toAnyRef(o))
        else
          unsafeVarsArray(key).set(toAnyRef(o), next)

        true
      }

      def getAndSetObject(o: Any, offset: Long, next: Any) = {
        val key = offset.asInstanceOf[Int]
        val old = unsafeVarsArray(key).get(toAnyRef(o))

        if (next == null)
          unsafeVarsArray(key).delete(toAnyRef(o))
        else
          unsafeVarsArray(key).set(toAnyRef(o), next)

        if (js.isUndefined(old)) null
        else old
      }

      def getAndAddLong(o: Any, offset: Long, next: Long) = {
        val key = offset.asInstanceOf[Int]
        val old = unsafeVarsArray(key).get(toAnyRef(o))

        val _next: Long =
          if (js.isUndefined(old)) next
          else old.asInstanceOf[Long] + next

        unsafeVarsArray(key).set(toAnyRef(o), _next)

        if (js.isUndefined(old)) 0L
        else old.asInstanceOf[Long]
      }

      def getAndAddInt(o: Any, offset: Long, next: Int) = {
        val key = offset.asInstanceOf[Int]
        val old = unsafeVarsArray(key).get(toAnyRef(o))

        val _next: Int =
          if (js.isUndefined(old)) next
          else old.asInstanceOf[Int] + next

        unsafeVarsArray(key).set(toAnyRef(o), _next)

        if (js.isUndefined(old)) 0
        else old.asInstanceOf[Int]
      }

      def putObjectVolatile(o: Any, offset: Long, next: Any) = {
        val key = offset.asInstanceOf[Int]

        unsafeVarsArray(key).set(toAnyRef(o), next)
      }
    }
}
