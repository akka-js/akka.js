/**
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.util
import scala.util.control.NonFatal
import scala.collection.immutable
import scala.annotation.tailrec
import scala.util.Try

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
  	  case _ =>
  	  	false
  	}  
  }
}
