package akka.actor

import scala.collection.immutable
import scala.reflect.ClassTag
import scala.util.Try
import scala.scalajs.js.annotation
import scala.collection.mutable

class JSDynamicAccess(val classLoader: ClassLoader) extends DynamicAccess {

  def this() = this(null)

	import scala.scalajs.js
  import scala.scalajs.reflect._

	def getRuntimeClass[A](name: String): InstantiatableClass = {
    Reflect.lookupInstantiatableClass(name).getOrElse {
      throw new InstantiationError(s"JSDynamicAccess $name is not js instantiable class")
    }
  }

  def newRuntimeInstance[A: ClassTag](dyn: InstantiatableClass)(args: immutable.Seq[(Class[_], AnyRef)]): A = {
     try {
       val constructorClasses = args.map(_._1)
       dyn.declaredConstructors.find(_.parameterTypes == constructorClasses).map{ ctor =>
         ctor.newInstance(args.map(_._2): _*).asInstanceOf[A]
       }.getOrElse{
         throw new InstantiationError("error trying to get instance for " + dyn.runtimeClass.getName + "\n" + dyn.toString)
       }
     } catch {
       case err: Exception => err.printStackTrace()
         throw err
     }
  }

  override def getClassFor[T: ClassTag](fqcn: String): Try[Class[_ <: T]] =
    Try[Class[_ <: T]]({
      getRuntimeClass(fqcn).runtimeClass.asInstanceOf[Class[_ <: T]]
    })

  override def createInstanceFor[T: ClassTag](clazz: Class[_], args: immutable.Seq[(Class[_], AnyRef)]): Try[T] =
    Try {
      val cls = getRuntimeClass[T](clazz.getName)
      val obj = newRuntimeInstance[T](cls)(args)
      val t = implicitly[ClassTag[T]].runtimeClass
      if (t.isInstance(obj)) obj.asInstanceOf[T] else throw new ClassCastException(clazz.getName + " is not a subtype of " + t)
    } recover
    { case e: Exception ⇒ throw e }

  override def createInstanceFor[T: ClassTag](fqcn: String, args: immutable.Seq[(Class[_], AnyRef)]): Try[T] =
    Try {
      newRuntimeInstance(getRuntimeClass(fqcn))(args)
    } recover
    { case e: Exception ⇒ throw e }

  override def getObjectFor[T: ClassTag](fqcn: String): Try[T] = {
    Try {
      Reflect.lookupLoadableModuleClass(fqcn).get.loadModule().asInstanceOf[T]
    } recover
    { case e: Exception ⇒ throw e }

  }
}

class ReflectiveDynamicAccess(cl: ClassLoader) extends JSDynamicAccess() {}
