/**
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.util
import scala.util.control.NonFatal
import scala.collection.immutable
import scala.annotation.tailrec
import scala.util.Try
import scala.scalajs.js

/**
 *
 * INTERNAL API
 */
private[akka] object Reflect {

  protected[akka] final def lookupAndSetField(clazz: Class[_], instance: AnyRef, name: String, value: Any): Boolean = {
    //Now we manage only IR patched classes
    import akka.actor._

    type PatchedActorCell = {
      var props: Props
    }

    type PatchedActor = {
      var context: ActorContext
      var self: ActorRef
    }

    try {
      name match {
        case "props" =>
          instance.asInstanceOf[PatchedActorCell].props = value.asInstanceOf[Props]
          true
        case "context" =>
          instance.asInstanceOf[PatchedActor].context = value.asInstanceOf[ActorContext]
          true
        case "self" =>
          instance.asInstanceOf[PatchedActor].self = value.asInstanceOf[ActorRef]
          true
        case any =>
          false
      }
    } catch {
      case err : Throwable =>
        //this is dirty and should be improved...
        s"$value".equals("null") || s"$value".endsWith("deadLetters]")
    } 
  }

  /**
   * INTERNAL API
   * @param clazz the class which to instantiate an instance of
   * @return a new instance from the default constructor of the given class
   */
  private[akka] def instantiate[T](clazz: Class[T]): T = 
    try instantiate[T](findConstructor(clazz, immutable.Seq[Any]()), immutable.Seq[Any]())
    catch {
      case iae: IllegalAccessException ⇒
        throw new IllegalArgumentException(s"cannot instantiate actor", iae)
    }

  /**
   * INTERNAL API
   * Calls findConstructor and invokes it with the given arguments.
   */
  private[akka] def instantiate[T](clazz: Class[T], args: immutable.Seq[Any]): T = {
    instantiate(findConstructor(clazz, args), args)
  }

  /**
   * INTERNAL API
   * Invokes the constructor with the given arguments.
   */
  private[akka] def instantiate[T](constructor: js.Dynamic, args: immutable.Seq[Any]): T = {
    try js.Dynamic.newInstance(constructor)(args.map(_.asInstanceOf[js.Any]): _*).asInstanceOf[T]
    catch {
      case e: IllegalArgumentException ⇒
        val argString = args mkString ("[", ", ", "]")
        throw new IllegalArgumentException(s"constructor $constructor is incompatible with arguments $argString", e)
    }
  }  

  /**
   * INTERNAL API
   * Implements a primitive form of overload resolution a.k.a. finding the
   * right constructor.
   */
  private[akka] def findConstructor[T](clazz: Class[T], args: immutable.Seq[Any]): js.Dynamic = {
    clazz.getName.split("\\.").foldLeft(scala.scalajs.runtime.environmentInfo.exportsNamespace){
      (prev, part) =>
        prev.selectDynamic(part)
      }
  }
}
