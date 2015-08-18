/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.actor

import scala.collection.immutable
import scala.reflect.ClassTag
import scala.util.Try
import scala.scalajs.js.annotation
import akka.testkit.TestKit
import akka.testkit.TestEventListener
import akka.event.LogExt
import akka.worker.WorkerActorRefProvider

@annotation.JSExportDescendentClasses
class JSDynamicAccess(/**val classLoader: ClassLoader*/) extends DynamicAccess {
	import scala.scalajs.js

  def classLoader: ClassLoader = ???

	def getRuntimeClass[A](name: String): js.Dynamic = {

     val ctor =
       name.split("\\.").foldLeft(scala.scalajs.runtime.environmentInfo.exportsNamespace){
         (prev, part) =>
            prev.selectDynamic(part)
         }

     ctor// @note IMPLEMENT IN SCALA.JS.asInstanceOf[Class[A]]
  	}

  	def newRuntimeInstance[A](dyn: js.Dynamic)(args: Any*): A = {
     try {
       val res = js.Dynamic.newInstance(dyn)(args.map(_.asInstanceOf[js.Any]): _*).asInstanceOf[A]
       res
     } catch {
       case err: Exception => err.printStackTrace()
         throw err
     }
  }
  private val map = scala.collection.mutable.HashMap(
    "akka.testkit.TestEventListener" -> classOf[akka.testkit.TestEventListener],
    "akka.actor.LocalActorRefProvider" -> classOf[akka.actor.LocalActorRefProvider],
    "akka.worker.WorkerActorRefProvider" -> classOf[akka.worker.WorkerActorRefProvider],
    "akka.event.LogExt" -> classOf[akka.event.LogExt],
    "akka.event.DefaultLogger" -> classOf[akka.event.DefaultLogger]
  )
  override def getClassFor[T: ClassTag](fqcn: String): Try[Class[_ <: T]] =
    Try[Class[_ <: T]]({
      /*val c = getRuntimeClass[T](fqcn)
      val t = implicitly[ClassTag[T]].runtimeClass
      if (t.isAssignableFrom(c)) c else throw new ClassCastException(t + " is not assignable from " + c)*/
      map(fqcn).asInstanceOf[Class[T]]
    })

  override def createInstanceFor[T: ClassTag](clazz: Class[_], args: immutable.Seq[(Class[_], AnyRef)]): Try[T] =
    Try {
      val types = args.map(_._1).toArray
      val values = args.map(_._2).toArray
      val cls = getRuntimeClass[T](clazz.getName)
      //val constructor = clazz.getDeclaredConstructor(types: _*)
      //constructor.setAccessible(true)
      val obj = //constructor.newInstance(values: _*)
      	newRuntimeInstance[T](cls.asInstanceOf[js.Dynamic])(values: _ *)
      val t = implicitly[ClassTag[T]].runtimeClass
      if (t.isInstance(obj)) obj.asInstanceOf[T] else throw new ClassCastException(clazz.getName + " is not a subtype of " + t)
    } recover /** @note IMPLEMENT IN SCALA.JS { case i: InvocationTargetException if i.getTargetException ne null ⇒ throw i.getTargetException } */
     { case e: Exception ⇒ throw e }

  override def createInstanceFor[T: ClassTag](fqcn: String, args: immutable.Seq[(Class[_], AnyRef)]): Try[T] =
    getClassFor(fqcn) flatMap { c ⇒ createInstanceFor(c, args) }

  override def getObjectFor[T: ClassTag](fqcn: String): Try[T] = {
    val splitted = fqcn.split("\\.").reverse
    val objName = {
      val name = splitted.head
      if (name.last == '$') name.dropRight(1)
      else name
    }
    val packageName = splitted.tail.reverse

    val obj =
        (packageName.foldLeft(scala.scalajs.runtime.environmentInfo.exportsNamespace){
         (prev, part) =>
            prev.selectDynamic(part)
         }).applyDynamic(objName)()

    Try {
      obj.asInstanceOf[T]
    }
  }
}
