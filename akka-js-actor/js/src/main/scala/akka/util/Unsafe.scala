package akka.util

import scala.collection.mutable

object Unsafe {

    val unsafeVars: mutable.HashMap[Long, Any] = mutable.HashMap()

    final val instance = new {

      def getObjectVolatile(o: Any, offset: Long) =
        unsafeVars.get(offset).getOrElse(null)

      def compareAndSwapObject(o: Any, offset: Long, old: Any, next: Any) = {
        unsafeVars(offset) = next
        true
      }

    }
}
