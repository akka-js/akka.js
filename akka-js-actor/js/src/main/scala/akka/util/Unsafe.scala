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
        o.asInstanceOf[WithUnsafe].unsafe = new Array[AnyRef](12)
    }

    final val instance = new {

      @inline
      def getObjectVolatile(o: Any, offset: Int): AnyRef = {
        try {
          Option(o.asInstanceOf[WithUnsafe].unsafe(offset)).getOrElse(
            fallback(offset)
          )
        } catch {
          case _: Throwable =>
            initIfNull(o)
            fallback(offset)
        }
      }

      @inline
      def compareAndSwapObject(o: Any, offset: Int, old: Any, next: Any): Boolean = {
        try {
          Option(o.asInstanceOf[WithUnsafe].unsafe(offset)) match {
            case Some(x) if x == old =>
              o.asInstanceOf[WithUnsafe].unsafe(offset) = next.asInstanceOf[AnyRef]
              true
            case None if old == fallback(offset) =>
              o.asInstanceOf[WithUnsafe].unsafe(offset) = next.asInstanceOf[AnyRef]
              true
            case _ =>
              false
          }
        } catch {
          case _: Throwable =>
            initIfNull(o)
            if (old == fallback(offset)) {
              o.asInstanceOf[WithUnsafe].unsafe(offset) = next.asInstanceOf[AnyRef]
              true
            } else false
        }
      }

      @inline
      def getAndSetObject(o: Any, offset: Int, next: Any): Any = {
        try {
          val res =
            Option(o.asInstanceOf[WithUnsafe].unsafe(offset)).getOrElse(fallback(offset))

          o.asInstanceOf[WithUnsafe].unsafe(offset) = next.asInstanceOf[AnyRef]

          res
        } catch {
          case _: Throwable =>
            initIfNull(o)
            o.asInstanceOf[WithUnsafe].unsafe(offset) = next.asInstanceOf[AnyRef]
            fallback(offset)
        }
      }

      @inline
      def getAndAddLong(o: Any, offset: Int, next: Long): Long = {
        try {
          val res: Long =
            o.asInstanceOf[WithUnsafe].unsafe(offset).asInstanceOf[Long]

          val value: Long = res + next
            o.asInstanceOf[WithUnsafe].unsafe(offset) = value.asInstanceOf[AnyRef]

          res
        } catch {
          case _: Throwable =>
            initIfNull(o)
            o.asInstanceOf[WithUnsafe].unsafe(offset) = next.asInstanceOf[AnyRef]
            0L
        }
      }

      @inline
      def getAndAddInt(o: Any, offset: Int, next: Int): Int = {
        try {
          val res =
            o.asInstanceOf[WithUnsafe].unsafe(offset).asInstanceOf[Int]

          val value: Int = res + next
            o.asInstanceOf[WithUnsafe].unsafe(offset) = value.asInstanceOf[AnyRef]

          res
        } catch {
          case _: Throwable =>
            initIfNull(o)
            o.asInstanceOf[WithUnsafe].unsafe(offset) = next.asInstanceOf[AnyRef]
            0
        }
      }

      @inline
      def putObjectVolatile(o: Any, offset: Int, next: Any): Unit = {
        try {
          o.asInstanceOf[WithUnsafe].unsafe(offset) = next.asInstanceOf[AnyRef]
        } catch {
          case _: Throwable =>
            initIfNull(o)
            o.asInstanceOf[WithUnsafe].unsafe(offset) = next.asInstanceOf[AnyRef]
        }
      }
    }
}
