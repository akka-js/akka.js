package org.scalajs.spickling
package test

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{ literal => lit }

case class Person(name: String, age: Int)

object CaseClassPicklersTest extends PicklersTest {

  PicklerRegistry.register[Person]

  describe("Case class picklers") {

    it("should be able to pickle a Person") {
      expectPickleEqual(
          Person("Jack", 24),
          lit(t = "org.scalajs.spickling.test.Person", v = lit(
              name = lit(t = "java.lang.String", v = "Jack"),
              age = lit(t = "java.lang.Integer", v = 24))))
    }

    it("should be able to unpickle a Person") {
      expectUnpickleEqual(
          lit(t = "org.scalajs.spickling.test.Person", v = lit(
              name = lit(t = "java.lang.String", v = "Jack"),
              age = lit(t = "java.lang.Integer", v = 24))),
          Person("Jack", 24))
    }
  }
}
