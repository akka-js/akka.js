/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.dispatch

import java.util.Collection
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.{ AtomicReference, AtomicLong }

import java.util.concurrent.{ TimeUnit, Callable, ExecutorService }

import scala.concurrent.duration.Duration

/**
 * Function0 without the fun stuff (mostly for the sake of the Java API side of things)
 */
trait ExecutorServiceFactory {
  def createExecutorService: ExecutorService
}

/**
 * Generic way to specify an ExecutorService to a Dispatcher, create it with the given name if desired
 */
trait ExecutorServiceFactoryProvider {
  def createExecutorServiceFactory(id: String/** @note IMPLEMENT IN SCALA.JS , threadFactory: ThreadFactory*/): ExecutorServiceFactory
}

object MonitorableThreadFactory {
  val doNothing: Thread.UncaughtExceptionHandler =
    new Thread.UncaughtExceptionHandler() { def uncaughtException(thread: Thread, cause: Throwable) = () }
}

case class MonitorableThreadFactory(name: String,
                                    daemonic: Boolean,
                                    contextClassLoader: Option[ClassLoader],
                                    exceptionHandler: Thread.UncaughtExceptionHandler = MonitorableThreadFactory.doNothing,
                                    protected val counter: AtomicLong = new AtomicLong)
  extends ThreadFactory /*with ForkJoinPool.ForkJoinWorkerThreadFactory*/ {

  def newThread(pool: Any/*ForkJoinPool*/)/*: ForkJoinWorkerThread*/ = {
    Thread.currentThread
  }

  class JsThread(runnable: Runnable) extends Thread {

    def start() = runnable.run

  }

  def newThread(runnable: Runnable): Thread = new JsThread(runnable)

  def withName(newName: String): MonitorableThreadFactory = copy(newName)

  protected def wire[T <: Thread](t: T): T = {
    t
  }

}


/**
 * As the name says
 */
trait ExecutorServiceDelegate extends ExecutorService {

  def executor: ExecutorService

  def execute(command: Runnable) = executor.execute(command)

  def shutdown() { executor.shutdown() }

  def shutdownNow() = executor.shutdownNow()

  def isShutdown = executor.isShutdown

  def isTerminated = executor.isTerminated

  def awaitTermination(l: Long, timeUnit: TimeUnit) = executor.awaitTermination(l, timeUnit)

  def submit[T](callable: Callable[T]) = executor.submit(callable)

  def submit[T](runnable: Runnable, t: T) = executor.submit(runnable, t)

  def submit(runnable: Runnable) = executor.submit(runnable)

  def invokeAll[T](callables: Collection[_ <: Callable[T]]) = executor.invokeAll(callables)

  def invokeAll[T](callables: Collection[_ <: Callable[T]], l: Long, timeUnit: TimeUnit) = executor.invokeAll(callables, l, timeUnit)

  def invokeAny[T](callables: Collection[_ <: Callable[T]]) = executor.invokeAny(callables)

  def invokeAny[T](callables: Collection[_ <: Callable[T]], l: Long, timeUnit: TimeUnit) = executor.invokeAny(callables, l, timeUnit)
}
