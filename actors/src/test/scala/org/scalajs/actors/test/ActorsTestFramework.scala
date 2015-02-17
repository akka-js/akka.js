package org.scalajs.actors.test

import scala.scalajs.js
import scala.scalajs.js.Dynamic.global
import scala.scalajs.js.JavaScriptException

//import scala.scalajs.test._
import utest._
import utest.framework._


object ActorsTestFramework extends TestSuite {
  def withEventQueue(body: => Unit): Unit = {
    val eventQueue = new scala.collection.mutable.Queue[js.Function0[_]]

    val oldSetTimeout = global.setTimeout
    val oldClearTimeout = global.clearTimeout
    val oldSetInterval = global.setInterval
    val oldClearInterval = global.clearInterval

    var lastID: Number = 0
    global.setTimeout = { (f: js.Function0[_], delay: Number) =>
      eventQueue.enqueue(f)
      lastID = lastID.intValue() + 1
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

  override def runTests(testOutput: Result, args: Array[String])(
    tests: js.Function0[Unit]): Unit = {
    withEventQueue {
      try {
        tests()
      } catch {
        case e: Throwable => throw new Exception(e)//testOutput.error(e.getMessage, e.getStackTrace)
      }
    }
  }
}
