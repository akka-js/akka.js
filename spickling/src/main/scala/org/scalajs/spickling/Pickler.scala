package org.scalajs.spickling

import scala.scalajs.js

trait Pickler[A] {
  type Picklee = A

  def pickle(obj: Picklee)(implicit registry: PicklerRegistry): js.Any
}

object Pickler {
  implicit object BooleanPickler extends Pickler[Boolean] {
    def pickle(x: Boolean)(implicit registry: PicklerRegistry): js.Any = x
  }

  implicit object CharPickler extends Pickler[Char] {
    def pickle(x: Char)(implicit registry: PicklerRegistry): js.Any = x.toString
  }

  implicit object BytePickler extends Pickler[Byte] {
    def pickle(x: Byte)(implicit registry: PicklerRegistry): js.Any = x
  }

  implicit object ShortPickler extends Pickler[Short] {
    def pickle(x: Short)(implicit registry: PicklerRegistry): js.Any = x
  }

  implicit object IntPickler extends Pickler[Int] {
    def pickle(x: Int)(implicit registry: PicklerRegistry): js.Any = x
  }

  implicit object LongPickler extends Pickler[Long] {
    def pickle(x: Long)(implicit registry: PicklerRegistry): js.Any = {
      val rx = scala.scalajs.runtime.Long.toRuntimeLong(x)
      js.Dynamic.literal(l = rx.l, m = rx.m, h = rx.h)
    }
  }

  implicit object FloatPickler extends Pickler[Float] {
    def pickle(x: Float)(implicit registry: PicklerRegistry): js.Any = x
  }

  implicit object DoublePickler extends Pickler[Double] {
    def pickle(x: Double)(implicit registry: PicklerRegistry): js.Any = x
  }

  implicit object StringPickler extends Pickler[String] {
    def pickle(x: String)(implicit registry: PicklerRegistry): js.Any = x
  }
}
