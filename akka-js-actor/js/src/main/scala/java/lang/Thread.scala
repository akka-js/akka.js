// should be a PR upstream
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

/* We need a constructor to create SingleThread in the companion object, but
 * we don't want user code doing a 'new Thread()' to link, because that could
 * be confusing.
 * So we use a binary signature that no Java source file can ever produce.
 */
class Thread private (dummy: Unit) extends Runnable {
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

  def getStackTrace(): Array[StackTraceElement] =
    Array()
    // java.lang.StackTrace.getCurrentStackTrace()

  def getId(): scala.Long = 1
}

object Thread {
  private[this] val SingleThread = new Thread(())

  def currentThread(): Thread = SingleThread

  def interrupted(): scala.Boolean = {
    val ret = currentThread.isInterrupted
    currentThread.interruptedState = false
    ret
  }

  trait UncaughtExceptionHandler {
    def uncaughtException(t: java.lang.Thread, e: Throwable): Unit
  }
}
