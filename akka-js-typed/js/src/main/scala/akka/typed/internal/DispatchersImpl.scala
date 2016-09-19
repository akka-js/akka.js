/**
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com/>
 */
package akka.typed
package internal

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.ExecutionContextExecutorService
//import java.util.concurrent.Executors
import akka.event.LoggingAdapter
import scalajs.js.timers.setTimeout
import scalajs.js.Dynamic.global

class DispatchersImpl(settings: akka.actor.ActorSystem.Settings, log: LoggingAdapter) extends Dispatchers {
  private val ex: ExecutionContextExecutorService = new ExecutionContextExecutorService {
    private var _isShutdown = false

    def reportFailure(cause: Throwable): Unit = log.error(cause, "exception caught by default executor")
    def execute(command: Runnable): Unit = if (!_isShutdown) global.setTimeout({ () =>
      command.run()
    }, 0)

    def awaitTermination(x$1: Long, x$2: java.util.concurrent.TimeUnit): Boolean = ???
    def invokeAll[T](x$1: java.util.Collection[_ <: java.util.concurrent.Callable[T]], x$2: Long, x$3: java.util.concurrent.TimeUnit): java.util.List[java.util.concurrent.Future[T]] = ???
    def invokeAll[T](x$1: java.util.Collection[_ <: java.util.concurrent.Callable[T]]): java.util.List[java.util.concurrent.Future[T]] = ???
    def invokeAny[T](x$1: java.util.Collection[_ <: java.util.concurrent.Callable[T]], x$2: Long, x$3: java.util.concurrent.TimeUnit): T = ???
    def invokeAny[T](x$1: java.util.Collection[_ <: java.util.concurrent.Callable[T]]): T = ???
    def isShutdown(): Boolean = _isShutdown
    def isTerminated(): Boolean = _isShutdown
    def shutdown(): Unit = _isShutdown = true
    def shutdownNow(): java.util.List[Runnable] = {
      shutdown()
      new java.util.ArrayList[Runnable]()
    }
    def submit(x$1: Runnable): java.util.concurrent.Future[_] = ???
    def submit[T](x$1: Runnable, x$2: T): java.util.concurrent.Future[T] = ???
    def submit[T](x$1: java.util.concurrent.Callable[T]): java.util.concurrent.Future[T] = ???
  }
  def lookup(selector: DispatcherSelector): ExecutionContextExecutor = ex //FIXME respect selection... exaclty
  def shutdown(): Unit = ()
}
