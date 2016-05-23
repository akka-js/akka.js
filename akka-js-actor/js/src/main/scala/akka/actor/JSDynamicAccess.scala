package akka.actor

import scala.collection.immutable
import scala.reflect.ClassTag
import scala.util.Try
import scala.scalajs.js.annotation
import akka.event.LogExt
import scala.collection.mutable

object JSDynamicAccess {

  protected[akka] val additional_classes_map: mutable.HashMap[String, Class[_]] = mutable.HashMap()

  def injectClass[T](nc: (String, Class[T])) = 
    additional_classes_map += nc

} 

@annotation.JSExportDescendentClasses
class JSDynamicAccess(val classLoader: ClassLoader) extends DynamicAccess {

  def this() = this(null)

	import scala.scalajs.js

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

  /*
  here we are waiting for Scala.js official implementation
  */
  private val classes_map: mutable.HashMap[String, Class[_]] = mutable.HashMap(
    "akka.actor.LocalActorRefProvider" -> classOf[akka.actor.LocalActorRefProvider],
    "akka.actor.JSLocalActorRefProvider" -> classOf[akka.actor.JSLocalActorRefProvider],
    "akka.event.LogExt" -> classOf[akka.event.LogExt],
    "akka.event.DefaultLogger" -> classOf[akka.event.DefaultLogger],
    "akka.event.LoggingBusActor" -> classOf[akka.event.LoggingBusActor],
    "akka.event.DefaultLoggingFilter" -> classOf[akka.event.DefaultLoggingFilter],
    "akka.actor.EventLoopScheduler" -> classOf[akka.actor.EventLoopScheduler],
    "akka.actor.DefaultSupervisorStrategy" -> classOf[akka.actor.DefaultSupervisorStrategy]
  ) ++ JSDynamicAccess.additional_classes_map

  def injectClass[T](nc: (String, Class[T])) = 
    classes_map += nc

  override def getClassFor[T: ClassTag](fqcn: String): Try[Class[_ <: T]] =
    Try[Class[_ <: T]]({
      /*val c = getRuntimeClass[T](fqcn)
      val t = implicitly[ClassTag[T]].runtimeClass
      if (t.isAssignableFrom(c)) c else throw new ClassCastException(t + " is not assignable from " + c)*/
      classes_map(fqcn).asInstanceOf[Class[T]]
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

    Try {
    val obj =
        (packageName.foldLeft(scala.scalajs.runtime.environmentInfo.exportsNamespace){
         (prev, part) =>
            prev.selectDynamic(part)
         }).applyDynamic(objName)()

      obj.asInstanceOf[T]
    }
  }
}

class ReflectiveDynamicAccess(cl: ClassLoader) extends JSDynamicAccess() {}
