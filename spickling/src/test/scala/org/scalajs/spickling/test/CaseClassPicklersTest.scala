package org.scalajs.spickling
package test

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{ literal => lit }

case class Person(name: String, age: Int)

case object TrivialCaseObject

object CaseClassPicklersTest extends PicklersTest {

  PicklerRegistry.register[Person]
  PicklerRegistry.register(TrivialCaseObject)

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

    it("should be able to pickle TrivialCaseObject") {
      expectPickleEqual(
          TrivialCaseObject,
          lit(s = "org.scalajs.spickling.test.TrivialCaseObject$"))
    }

    it("should be able to unpickle TrivialCaseObject") {
      expectUnpickleEqual(
          lit(s = "org.scalajs.spickling.test.TrivialCaseObject$"),
          TrivialCaseObject)
    }
  }
}
