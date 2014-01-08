package org.scalajs.spickling
package test

import scala.reflect.{ ClassTag, classTag }

import scala.scalajs.js
import scala.scalajs.test.JasmineTest
import org.scalajs.jasmine._

trait PicklersTest extends JasmineTest {
  def expect_==(actual: Any, expected: Any): Unit = {
    // TODO Improve this, for example by creating my ExpectationResult myself
    if (actual == expected)
      expect(true).toEqual(true) // for stats
    else
      expect(actual.asInstanceOf[js.Any]).toEqual(expected.asInstanceOf[js.Any])
  }

  def expectPickleEqual(value: Any, expectedPickle: js.Any): Unit = {
    expect(PicklerRegistry.pickle(value)).toEqual(expectedPickle)
  }

  def expectUnpickleEqual(pickle: js.Any, expectedValue: Any): Unit = {
    expect_==(PicklerRegistry.unpickle(pickle), expectedValue)
  }
}
