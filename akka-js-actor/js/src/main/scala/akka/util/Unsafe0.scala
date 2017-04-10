package akka.util

import akka.actor.dungeon.AbstractActorCell
import akka.actor.dungeon.ChildrenContainer.EmptyChildrenContainer
import akka.actor.FunctionRef
import scalajs.js

object Unsafe_weak_array {

    val unsafeVars: WeakMap[AnyRef, js.Array[Any]] =
      new WeakMap[AnyRef, js.Array[Any]]()

    def fallback(offset: Long): AnyRef = {
      //Missing initializations...
      if (offset == AbstractActorCell.childrenOffset)
        EmptyChildrenContainer
      else if (offset == AbstractActorCell.nextNameOffset)
        0L.asInstanceOf[AnyRef]
      else if (offset == AbstractActorCell.functionRefsOffset)
        Map.empty[String, FunctionRef]
      else null.asInstanceOf[AnyRef]
    }

    @inline
    def toAnyRef(a: Any): AnyRef =
      a.asInstanceOf[AnyRef]

    @inline
    def initEntry(key: Any, offset: Long, value: Any) = {
      val arr = new js.Array[Any](100)
      arr(offset.asInstanceOf[Int]) = value
      unsafeVars.set(key.asInstanceOf[AnyRef], arr)
    }

    final val instance = new {

      def getObjectVolatile(o: Any, offset: Long): AnyRef = {
        val obj = unsafeVars.get(toAnyRef(o))

        if (js.isUndefined(obj)) fallback(offset)
        else {
          val res = obj(offset.asInstanceOf[Int])

          if (js.isUndefined(res)) fallback(offset)
          else res.asInstanceOf[AnyRef]
        }
      }

      def compareAndSwapObject(o: Any, offset: Long, old: Any, next: Any) = {
        val obj = unsafeVars.get(toAnyRef(o))

        if (js.isUndefined(obj) && old == null) {
          if (old == null) {
            initEntry(toAnyRef(obj), offset, next)
            true
          } else false
        } else {
          val res = obj(offset.asInstanceOf[Int])

          if (js.isUndefined(res) && old == null) {
            if (old == null) {
              obj(offset.asInstanceOf[Int]) = next
              true
            } else false
          } else {
            if (res == old) {
              obj(offset.asInstanceOf[Int]) = next
              true
            } else false
          }
        }
      }

      def getAndSetObject(o: Any, offset: Long, next: Any) = {
        val obj = unsafeVars.get(toAnyRef(o))

        if (js.isUndefined(obj)) {
          initEntry(o, offset, next)
          fallback(offset)
        } else {
          val res = obj(offset.asInstanceOf[Int])

          obj(offset.asInstanceOf[Int]) = next

          if (js.isUndefined(res))
            fallback(offset)
          else res
        }
      }

      def getAndAddLong(o: Any, offset: Long, next: Long) = {
        val obj = unsafeVars.get(toAnyRef(o))

        if (js.isUndefined(obj)) {
          initEntry(o, offset, next)
          0L
        } else {
          val res = obj(offset.asInstanceOf[Int])

          if (js.isUndefined(res)) {
            obj(offset.asInstanceOf[Int]) = next
            0L
          } else {
            obj(offset.asInstanceOf[Int]) = res.asInstanceOf[Long] + next
            res.asInstanceOf[Long]
          }
        }
      }

      def getAndAddInt(o: Any, offset: Long, next: Int) = {
        val obj = unsafeVars.get(toAnyRef(o))

        if (js.isUndefined(obj)) {
          initEntry(o, offset, next)
          0
        } else {
          val res = obj(offset.asInstanceOf[Int])

          if (js.isUndefined(res)) {
            obj(offset.asInstanceOf[Int]) = next
            0
          } else {
            obj(offset.asInstanceOf[Int]) = res.asInstanceOf[Int] + next
            res.asInstanceOf[Int]
          }
        }
      }

      def putObjectVolatile(o: Any, offset: Long, next: Any) = {
        val obj = unsafeVars.get(toAnyRef(o))

        if (js.isUndefined(obj))
          initEntry(o, offset, next)
        else
          obj(offset.asInstanceOf[Int]) = next
      }
    }
}
