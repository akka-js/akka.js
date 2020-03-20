// This class is copy pasted from Scala.js sources
// here: https://github.com/scala-js/scala-js/pull/3992
// the reasoning why it won't be merged to the core repository
// here we keep the diff that enable Akka.Js to reuse Akka code
/*
 * Scala.js (https://www.scala-js.org/)
 *
 * Copyright EPFL.
 *
 * Licensed under Apache License 2.0
 * (https://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package java.lang

class Thread extends Runnable {
  private var interruptedState = false
  private[this] var name: String = "main" // default name of the main thread

  def run(): Unit = ()

  def interrupt(): Unit =
    interruptedState = true

  def isInterrupted(): scala.Boolean =
    interruptedState

  final def setName(name: String): Unit =
    this.name = name

  final def getName(): String =
    this.name

  def getStackTrace(): Array[StackTraceElement] = Array()
    // StackTrace.getCurrentStackTrace()

  def getId(): scala.Long = 1

  def getUncaughtExceptionHandler(): Thread.UncaughtExceptionHandler = null

}

object Thread {
  private[this] val SingleThread = new Thread()

  def currentThread(): Thread = SingleThread

  def interrupted(): scala.Boolean = {
    val ret = currentThread.isInterrupted
    currentThread.interruptedState = false
    ret
  }

  trait UncaughtExceptionHandler {
    def uncaughtException(t: Thread, e: Throwable): Unit
  }
}
