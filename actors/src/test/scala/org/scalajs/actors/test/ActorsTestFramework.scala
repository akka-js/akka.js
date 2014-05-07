package org.scalajs.actors.test

import scala.scalajs.js
import scala.scalajs.js.Dynamic.global
import scala.scalajs.js.JavaScriptException

import scala.scalajs.test._

object ActorsTestFramework extends TestFramework {
  def withEventQueue(body: => Unit): Unit = {
    val eventQueue = new scala.collection.mutable.Queue[js.Function0[_]]

    val oldSetTimeout = global.setTimeout
    val oldClearTimeout = global.clearTimeout
    val oldSetInterval = global.setInterval
    val oldClearInterval = global.clearInterval

    var lastID: js.Number = 0
    global.setTimeout = { (f: js.Function0[_], delay: js.Number) =>
      eventQueue.enqueue(f)
      lastID += 1
      lastID
    }
    global.clearTimeout = { () => sys.error("Stub for clearTimeout") }
    global.setInterval  = { () => sys.error("Stub for setInterval") }
    global.clearInterval = { () => sys.error("Stub for clearInterval") }

    try {
      body

      while (eventQueue.nonEmpty) {
        val event = eventQueue.dequeue()
        event()
      }
    } finally {
      global.setTimeout = oldSetTimeout
      global.clearTimeout = oldClearTimeout
      global.setInterval = oldSetInterval
      global.clearInterval = oldClearInterval
    }
  }

  override def runTests(testOutput: TestOutput, args: js.Array[String])(
    tests: js.Function0[Unit]): Unit = {
    withEventQueue {
      try {
        tests()
      } catch {
        case e: Throwable => testOutput.error(e.getMessage, e.getStackTrace)
      }
    }
  }
}
