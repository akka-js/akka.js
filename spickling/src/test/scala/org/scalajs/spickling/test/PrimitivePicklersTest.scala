package org.scalajs.spickling
package test

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{ literal => lit }

object PrimitivePicklersTest extends PicklersTest {

  describe("Primitive picklers") {

    it("should be able to pickle a Boolean") {
      expectPickleEqual(
          true,
          lit(t = "java.lang.Boolean", v = true))
    }

    it("should be able to unpickle a Boolean") {
      expectUnpickleEqual(
          lit(t = "java.lang.Boolean", v = true),
          true)
    }

    it("should be able to pickle an Int") {
      expectPickleEqual(
          42,
          lit(t = "java.lang.Integer", v = 42))
    }

    it("should be able to unpickle an Int") {
      expectUnpickleEqual(
          lit(t = "java.lang.Integer", v = 42),
          42)
    }

    it("should be able to pickle a Long") {
      expectPickleEqual(
          42L,
          lit(t = "java.lang.Long", v = lit(l = 42, m = 0, h = 0)))
    }

    it("should be able to unpickle a Long") {
      expectUnpickleEqual(
          lit(t = "java.lang.Long", v = lit(l = 42, m = 0, h = 0)),
          42L)
    }

    it("should be able to pickle a String") {
      expectPickleEqual(
          "hello",
          lit(t = "java.lang.String", v = "hello"))
    }

    it("should be able to unpickle a String") {
      expectUnpickleEqual(
          lit(t = "java.lang.String", v = "hello"),
          "hello")
    }

    it("should be able to pickle null") {
      expectPickleEqual(null, null)
    }

    it("should be able to unpickle null") {
      expectUnpickleEqual(null, null)
    }
  }
}
