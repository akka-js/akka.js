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

    final val instance = new {

      def getObjectVolatile(o: Any, offset: Long): AnyRef = {
        unsafeVars.get((o.hashCode,offset.asInstanceOf[Int])).getOrElse(fallback(offset)).asInstanceOf[AnyRef]
      }

      def compareAndSwapObject(o: Any, offset: Long, old: Any, next: Any) = {
        unsafeVars.update((o.hashCode,offset.asInstanceOf[Int]), next)
        true
      }

      def getAndSetObject(o: Any, offset: Long, next: Any) = {
        val ret = unsafeVars.get((o.hashCode,offset.asInstanceOf[Int])).getOrElse(fallback(offset))
        unsafeVars.update((o.hashCode,offset.asInstanceOf[Int]), next)
        ret
      }

      def getAndAddLong(o: Any, offset: Long, next: Long) = {
        val ret = unsafeVars.get((o.hashCode,offset.asInstanceOf[Int])).map(_.asInstanceOf[Long]).getOrElse(0L)
        unsafeVars.update((o.hashCode,offset.toInt), ret + next)
        ret
      }

      def putObjectVolatile(o: Any, offset: Long, next: Any) = {
        unsafeVars.update((o.hashCode,offset.asInstanceOf[Int]), next)
      }

    }
}
