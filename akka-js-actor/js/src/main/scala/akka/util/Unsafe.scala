package akka.util

import scala.collection.mutable

import akka.actor.dungeon.AbstractActorCell
import akka.actor.dungeon.ChildrenContainer.EmptyChildrenContainer
import akka.actor.FunctionRef

object Unsafe {

    val unsafeVars: WeakMap[AnyRef, mutable.HashMap[Int, Any]] =
      new WeakMap[AnyRef, mutable.HashMap[Int, Any]]()

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

    def safeGet(m: WeakMap[AnyRef, mutable.HashMap[Int, Any]], k: Any):
      Option[mutable.HashMap[Int, Any]] = {
      if (m.has(k.asInstanceOf[AnyRef])) Some(m.get(k.asInstanceOf[AnyRef]))
      else None
    }

    final val instance = new {

      def getObjectVolatile(o: Any, offset: Long): AnyRef = {
        safeGet(unsafeVars, o)
          .map(_.get(offset.asInstanceOf[Int]))
          .flatten
          .getOrElse(fallback(offset)).asInstanceOf[AnyRef]
      }

      def compareAndSwapObject(o: Any, offset: Long, old: Any, next: Any) = {
        val key = offset.asInstanceOf[Int]
        if (next == null)
          safeGet(unsafeVars, o)
            .map(_.remove(key))
        else {
          val old = safeGet(unsafeVars, o)
          if (old.isDefined)
            old.get.update(key, next)
          else {
            val in = new mutable.HashMap[Int, Any]()
            in.update(key, next)
            unsafeVars.set(o.asInstanceOf[AnyRef], in)
          }
        }
        true
      }

      def getAndSetObject(o: Any, offset: Long, next: Any) = {
        val key = offset.asInstanceOf[Int]
        val ret = safeGet(unsafeVars, o)
          .map(_.get(key))
          .flatten
          .getOrElse(fallback(offset))

        if (next == null)
          safeGet(unsafeVars, o)
            .map(_.remove(key))
        else {
          val old = safeGet(unsafeVars, o)
          if (old.isDefined)
            old.get.update(key, next)
          else {
            val in = new mutable.HashMap[Int, Any]()
            in.update(key, next)
            unsafeVars.set(o.asInstanceOf[AnyRef], in)
          }
        }
        ret
      }

      def getAndAddLong(o: Any, offset: Long, next: Long) = {
        val key = offset.asInstanceOf[Int]
        val ret = safeGet(unsafeVars, o)
          .map(_.get(key))
          .flatten
          .map(_.asInstanceOf[Long])
          .getOrElse(0L)

        if (next == 0L)
          safeGet(unsafeVars, o)
            .map(_.remove(key))
        else {
          val old = safeGet(unsafeVars, o)
          if (old.isDefined)
            old.get.update(key, ret + next)
          else {
            val in = new mutable.HashMap[Int, Any]()
            in.update(key, next)
            unsafeVars.set(o.asInstanceOf[AnyRef], in)
          }
        }
        ret
      }

      def putObjectVolatile(o: Any, offset: Long, next: Any) = {
        val key = offset.asInstanceOf[Int]

        if (next == null)
          safeGet(unsafeVars, o)
            .map(_.remove(key))
        else {
          val old = safeGet(unsafeVars, o)
          if (old.isDefined)
            old.get.update(key, next)
          else {
            val in = new mutable.HashMap[Int, Any]()
            in.update(key, next)
            unsafeVars.set(o.asInstanceOf[AnyRef], in)
          }
        }
      }
    }
}
