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

  private[akka] def validate(clazz: Class[_]) = ()

  def isAbstract(clazz: Class[_]) = ()

  /**
   * Java API: create a Props given a class and its constructor arguments.
   */
  def create(clazz: Class[_], args: AnyRef*): Props = Props(clazz = clazz, args = args.toSeq)

}
