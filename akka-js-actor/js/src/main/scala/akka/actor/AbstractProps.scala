/**
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.actor

import akka.dispatch._
import akka.routing._

import scala.language.existentials
import scala.reflect.ClassTag

/**
 * Java API MOCKUP: Factory for Props instances.
 */
trait AbstractProps {

  def isAbstract(clazz: Class[_]) = ()

}
