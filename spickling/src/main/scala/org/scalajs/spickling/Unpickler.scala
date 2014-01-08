package org.scalajs.spickling

import scala.scalajs.js

trait Unpickler[A] {
  type Unpicklee = A

  def unpickle(json: js.Any)(implicit registry: PicklerRegistry): A
}

object Unpickler extends PicklerMaterializers {
  implicit object BooleanUnpickler extends Unpickler[Boolean] {
    def unpickle(json: js.Any)(implicit registry: PicklerRegistry): Boolean =
      json.asInstanceOf[js.Boolean]
  }

  implicit object CharUnpickler extends Unpickler[Char] {
    def unpickle(json: js.Any)(implicit registry: PicklerRegistry): Char =
      (json.asInstanceOf[js.String]: String).charAt(0)
  }

  implicit object ByteUnpickler extends Unpickler[Byte] {
    def unpickle(json: js.Any)(implicit registry: PicklerRegistry): Byte =
      json.asInstanceOf[js.Number].toByte
  }

  implicit object ShortUnpickler extends Unpickler[Short] {
    def unpickle(json: js.Any)(implicit registry: PicklerRegistry): Short =
      json.asInstanceOf[js.Number].toShort
  }

  implicit object IntUnpickler extends Unpickler[Int] {
    def unpickle(json: js.Any)(implicit registry: PicklerRegistry): Int =
      json.asInstanceOf[js.Number].toInt
  }

  implicit object LongUnpickler extends Unpickler[Long] {
    def unpickle(json: js.Any)(implicit registry: PicklerRegistry): Long = {
      val obj = json.asInstanceOf[js.Dynamic]
      scala.scalajs.runtime.Long.fromRuntimeLong(scala.scalajs.runtime.Long(
          obj.l.asInstanceOf[js.Number].toInt,
          obj.m.asInstanceOf[js.Number].toInt,
          obj.h.asInstanceOf[js.Number].toInt))
    }
  }

  implicit object FloatUnpickler extends Unpickler[Float] {
    def unpickle(json: js.Any)(implicit registry: PicklerRegistry): Float =
      json.asInstanceOf[js.Number].toFloat
  }

  implicit object DoubleUnpickler extends Unpickler[Double] {
    def unpickle(json: js.Any)(implicit registry: PicklerRegistry): Double =
      json.asInstanceOf[js.Number].toDouble
  }

  implicit object StringUnpickler extends Unpickler[String] {
    def unpickle(json: js.Any)(implicit registry: PicklerRegistry): String =
      json.asInstanceOf[js.String]
  }
}
