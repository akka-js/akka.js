package org.scalajs.spickling
package test

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{ literal => lit }

case class Person(name: String, age: Int)

object CaseClassPicklersTest extends PicklersTest {

  implicit object TempPseudoPersonUnpickler extends Unpickler[Person] {
    def unpickle(pickle: js.Any)(implicit registry: PicklerRegistry): Person = {
      Person("???", 0)
    }
  }

  PicklerRegistry.register[Person]

  describe("Case class picklers") {

    it("should be able to pickle a Person") {
      expectPickleEqual(
          Person("Jack", 24),
          lit(t = "org.scalajs.spickling.test.Person", v = lit(
              name = lit(t = "java.lang.String", v = "Jack"),
              age = lit(t = "java.lang.Integer", v = 24))))
    }
  }
}
