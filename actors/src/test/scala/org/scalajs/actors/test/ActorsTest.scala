package org.scalajs.actors.test

import scala.scalajs.js
import js.Dynamic.global

trait ActorsTest extends scala.scalajs.test.Test with DelayedInit {
  private var delayedInits: List[() => Unit] = Nil

  def delayedInit(body: => Unit): Unit =
    delayedInits ::= (() => body)

  def runTests(): Unit = {
    delayedInits.reverse.foreach(body => body())
  }
}
