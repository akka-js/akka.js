package akka.util

import scala.collection.mutable

object Unsafe {

    val unsafeVars: mutable.HashMap[(Long,Long), Any] = mutable.HashMap()

    final val instance = new {

      def getObjectVolatile(o: Any, offset: Long) =
        unsafeVars.get((o.hashCode,offset)).getOrElse(null)

      def compareAndSwapObject(o: Any, offset: Long, old: Any, next: Any) = {
        unsafeVars((o.hashCode,offset)) = next
        true
      }

      def getAndSetObject(o: Any, offset: Long, next: Any) = {
        val ret = unsafeVars.get((o.hashCode,offset)).getOrElse(null)
        unsafeVars((o.hashCode,offset)) = next
        ret
      }

    }
}
