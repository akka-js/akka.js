package akka.util

import scala.collection.mutable

import akka.actor.dungeon.AbstractActorCell
import akka.actor.dungeon.ChildrenContainer.EmptyChildrenContainer
import akka.actor.FunctionRef

object Unsafe {

    val unsafeVars: mutable.HashMap[(Int,Int), Any] = mutable.HashMap()

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

    def safeHashCode(a: Any): Int = {
      a match {
        case rar: akka.actor.RepointableActorRef =>
          rar.path.uid
        case ap: akka.pattern.PromiseActorRef =>
          ap.provider.tempPath().uid
        case _ =>
          a.hashCode()
      }
    }

    final val instance = new {

      def getObjectVolatile(o: Any, offset: Long): AnyRef = {
        unsafeVars.get((safeHashCode(o),offset.asInstanceOf[Int])).getOrElse(fallback(offset)).asInstanceOf[AnyRef]
      }

      def compareAndSwapObject(o: Any, offset: Long, old: Any, next: Any) = {
        val key = (safeHashCode(o),offset.asInstanceOf[Int])
        if (next == null)
          unsafeVars.remove(key)
        else
          unsafeVars.update(key, next)
        true
      }

      def getAndSetObject(o: Any, offset: Long, next: Any) = {
        val key = (safeHashCode(o),offset.asInstanceOf[Int])
        val ret = unsafeVars.get(key).getOrElse(fallback(offset))
        if (next == null)
          unsafeVars.remove(key)
        else
          unsafeVars.update(key, next)
        ret
      }

      def getAndAddLong(o: Any, offset: Long, next: Long) = {
        val key = (safeHashCode(o),offset.asInstanceOf[Int])
        val ret = unsafeVars.get(key).map(_.asInstanceOf[Long]).getOrElse(0L)
        if (next == 0L)
          unsafeVars.remove(key)
        else
          unsafeVars.update(key, ret + next)
        ret
      }

      def putObjectVolatile(o: Any, offset: Long, next: Any) = {
        val key = (safeHashCode(o),offset.asInstanceOf[Int])
        if (next == null)
          unsafeVars.remove(key)
        else
          unsafeVars.update(key, next)
      }

    }
}
