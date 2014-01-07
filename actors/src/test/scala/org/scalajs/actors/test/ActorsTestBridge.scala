package org.scalajs.actors.test

import scala.scalajs.js
import js.Dynamic.{ newInstance, global }

import scala.scalajs.test._

class ActorsTestBridge(reporter: EventProxy, framework: String, test: String) {
  val testOutput = new TestOutputBridge(reporter)

  val testFramework =
    global.ScalaJS.modules.applyDynamic(framework)()
      .asInstanceOf[TestFramework]

  testFramework.runTests(testOutput) {
    global.ScalaJS.modules.applyDynamic(test)().runTests__V()
  }
}
