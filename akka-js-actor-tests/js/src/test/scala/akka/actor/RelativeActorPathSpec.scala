/**
 * Copyright (C) 2009-2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.actor

import org.scalatest.WordSpec
import org.scalatest.Matchers
import scala.scalajs.js.URIUtils
import scala.collection.immutable

class RelativeActorPathSpec extends WordSpec with Matchers {

  def elements(path: String): immutable.Seq[String] = RelativeActorPath.unapply(path).getOrElse(Nil)

  "RelativeActorPath" must {
    "match single name" in {
      elements("foo") should ===(List("foo"))
    }
    "match path separated names" in {
      elements("foo/bar/baz") should ===(List("foo", "bar", "baz"))
    }
    "match url encoded name" in {
      val name = URIUtils.encodeURI("akka://ClusterSystem@127.0.0.1:2552")
      elements(name) should ===(List(name))
    }
    "match path with uid fragment" in {
      elements("foo/bar/baz#1234") should ===(List("foo", "bar", "baz#1234"))
    }
  }
}
