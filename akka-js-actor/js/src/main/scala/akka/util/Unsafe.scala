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

      def getObjectVolatile(o: Any, offset: Long) = {
        unsafeVars.get((o.hashCode,offset.toInt)).getOrElse(fallback(offset))
      }

      def compareAndSwapObject(o: Any, offset: Long, old: Any, next: Any) = {
        unsafeVars((o.hashCode,offset.toInt)) = next
        true
      }

      def getAndSetObject(o: Any, offset: Long, next: Any) = {
        val ret = unsafeVars.get((o.hashCode,offset.toInt)).getOrElse(fallback(offset))
        unsafeVars((o.hashCode,offset.toInt)) = next
        ret
      }

      def getAndAddLong(o: Any, offset: Long, next: Long) = {
        val ret = unsafeVars.get((o.hashCode,offset.toInt)).map(_.asInstanceOf[Long]).getOrElse(0L)
        unsafeVars((o.hashCode,offset.toInt)) = ret + next
        ret
      }

      def putObjectVolatile(o: Any, offset: Int, next: Any) = {
        unsafeVars((o.hashCode,offset.toInt)) = next
      }

    }
}
