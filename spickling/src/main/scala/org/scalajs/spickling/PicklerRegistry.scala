package org.scalajs.spickling

import scala.reflect.ClassTag
import scala.collection.mutable

import scala.scalajs.js

object PicklerRegistry extends PicklerRegistry {
  class SingletonFullName[A](val name: String)

  object SingletonFullName extends PicklerMaterializers
}

class PicklerRegistry {
  import PicklerRegistry._

  private val picklers = new mutable.HashMap[String, Pickler[_]]
  private val unpicklers = new mutable.HashMap[String, Unpickler[_]]
  private val singletons = new mutable.HashMap[Any, String]
  private val singletonsRev = new mutable.HashMap[String, Any]

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

  def register[A <: Singleton](obj: A)(implicit name: SingletonFullName[A]): Unit = {
    singletons(obj) = name.name
    singletonsRev(name.name) = obj
  }

  def pickle(value: Any): js.Any = {
    if (value == null) {
      null
    } else {
      singletons.get(value) match {
        case Some(name) => js.Dynamic.literal(s = name)
        case _ =>
          val className = value.getClass.getName
          val pickler = picklers(className)
          val pickledValue = pickler.pickle(value.asInstanceOf[pickler.Picklee])
          js.Dynamic.literal(
              t = className,
              v = pickledValue)
          }
    }
  }

  def unpickle(json: js.Any): Any = {
    if (json eq null) {
      null
    } else {
      val dyn = json.asInstanceOf[js.Dynamic]
      if (!(!dyn.s)) {
        singletonsRev(dyn.s.asInstanceOf[js.String])
      } else {
        val className: String = dyn.t.asInstanceOf[js.String]
        val unpickler = unpicklers(className)
        unpickler.unpickle(dyn.v)
      }
    }
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
