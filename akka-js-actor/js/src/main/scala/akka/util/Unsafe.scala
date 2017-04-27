package akka.util

import akka.actor.dungeon.AbstractActorCell
import akka.actor.dungeon.ChildrenContainer.EmptyChildrenContainer
import akka.actor.FunctionRef
import scalajs.js
import scala.collection.mutable

object Unsafe {

    type WithUnsafe = {
      var unsafe: Array[AnyRef]
    }

    def fallback(offset: Int): AnyRef = {
      //Missing initializations...
      {
        if (offset == AbstractActorCell.childrenOffset)
          EmptyChildrenContainer
        else if (offset == AbstractActorCell.nextNameOffset)
          0L
        else if (offset == AbstractActorCell.functionRefsOffset)
          Map.empty[String, FunctionRef]
        else null
      }.asInstanceOf[AnyRef]
    }

    @inline
    def initIfNull(o: Any) = {
      if (o.asInstanceOf[WithUnsafe].unsafe eq null)
        o.asInstanceOf[WithUnsafe].unsafe = new Array[AnyRef](10)
    }

    final val instance = new {

      @inline
      def getObjectVolatile(o: Any, offset: Int): AnyRef = {
        initIfNull(o)

        Option(o.asInstanceOf[WithUnsafe].unsafe(offset)).getOrElse(
          fallback(offset)
        )
      }

      @inline
      def compareAndSwapObject(o: Any, offset: Int, old: Any, next: Any): Boolean = {
        initIfNull(o)
        //println("qui "+offset+"  "+ next)
        //println(o.asInstanceOf[WithUnsafe].unsafe)
        //println(o.asInstanceOf[WithUnsafe].unsafe(offset))

        Option(o.asInstanceOf[WithUnsafe].unsafe(offset)) match {
          case Some(x) if x == old =>
            //println("1")
            o.asInstanceOf[WithUnsafe].unsafe(offset) = next.asInstanceOf[AnyRef]
            true
          case None if old == fallback(offset) =>
            //println("2")
            o.asInstanceOf[WithUnsafe].unsafe(offset) = next.asInstanceOf[AnyRef]
            true
          case _ =>
            //println("old "+old )
            //println("true "+(old == null))
            //println("undef "+ js.isUndefined(old))
            false
        }
      }

      @inline
      def getAndSetObject(o: Any, offset: Int, next: Any): Any = {
        initIfNull(o)

        val res =
          Option(o.asInstanceOf[WithUnsafe].unsafe(offset)).getOrElse(fallback(offset))

        o.asInstanceOf[WithUnsafe].unsafe(offset) = next.asInstanceOf[AnyRef]

        res
      }

      @inline
      def getAndAddLong(o: Any, offset: Int, next: Long): Long = {
        initIfNull(o)

        val res: Long =
          o.asInstanceOf[WithUnsafe].unsafe(offset).asInstanceOf[Long]

        val value: Long = res + next
        o.asInstanceOf[WithUnsafe].unsafe(offset) = value.asInstanceOf[AnyRef]

        res
      }

      @inline
      def getAndAddInt(o: Any, offset: Int, next: Int): Int = {
        initIfNull(o)

        val res =
          o.asInstanceOf[WithUnsafe].unsafe(offset).asInstanceOf[Int]

        val value: Int = res + next
        o.asInstanceOf[WithUnsafe].unsafe(offset) = value.asInstanceOf[AnyRef]

        res
      }

      @inline
      def putObjectVolatile(o: Any, offset: Int, next: Any): Unit = {
        initIfNull(o)

        o.asInstanceOf[WithUnsafe].unsafe(offset) = next.asInstanceOf[AnyRef]
      }
    }
}
