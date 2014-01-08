package org.scalajs.spickling

import scala.reflect.ClassTag
import scala.collection.mutable

import scala.scalajs.js

object PicklerRegistry extends PicklerRegistry {
}

class PicklerRegistry {
  private val picklers = new mutable.HashMap[String, Pickler[_]]
  private val unpicklers = new mutable.HashMap[String, Unpickler[_]]

  implicit private def self = this

  registerBuiltinPicklers()

  private def registerInternal(clazz: Class[_], pickler: Pickler[_],
      unpickler: Unpickler[_]): Unit = {
    picklers(clazz.getName) = pickler
    unpicklers(clazz.getName) = unpickler
  }

  def register[A : ClassTag](pickler: Pickler[A],
      unpickler: Unpickler[A]): Unit = {
    registerInternal(implicitly[ClassTag[A]].runtimeClass, pickler, unpickler)
  }

  def register[A : ClassTag](implicit pickler: Pickler[A],
      unpickler: Unpickler[A]): Unit = {
    register(pickler, unpickler)
  }

  def pickle(value: Any): js.Any = {
    val className = value.getClass.getName
    val pickler = picklers(className)
    val pickledValue = pickler.pickle(value.asInstanceOf[pickler.Picklee])
    js.Dynamic.literal(
        t = className,
        v = pickledValue)
  }

  def unpickle(json: js.Any): Any = {
    val dyn = json.asInstanceOf[js.Dynamic]
    val className: String = dyn.t.asInstanceOf[js.String]
    val unpickler = unpicklers(className)
    unpickler.unpickle(dyn.v)
  }

  private def registerBuiltinPicklers(): Unit = {
    registerPrimitive[Boolean, java.lang.Boolean]
    registerPrimitive[Char, java.lang.Character]
    registerPrimitive[Byte, java.lang.Byte]
    registerPrimitive[Short, java.lang.Short]
    registerPrimitive[Int, java.lang.Integer]
    registerPrimitive[Long, java.lang.Long]
    registerPrimitive[Float, java.lang.Float]
    registerPrimitive[Double, java.lang.Double]

    register[String]
  }

  private def registerPrimitive[P : ClassTag, W : ClassTag](
      implicit pickler: Pickler[P], unpickler: Unpickler[P]): Unit = {
    register[P]
    registerInternal(implicitly[ClassTag[W]].runtimeClass, pickler, unpickler)
  }
}
