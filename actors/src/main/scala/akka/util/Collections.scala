package akka.util

import scala.collection.immutable

object Collections {

  case object EmptyImmutableSeq extends immutable.Seq[Nothing] {
    override final def iterator = Iterator.empty
    override final def apply(idx: Int): Nothing =
      throw new java.lang.IndexOutOfBoundsException(idx.toString)
    override final def length: Int = 0
  }

}
