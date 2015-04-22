package akka.dispatch

import java.util.Collection
import java.util.concurrent.{ TimeUnit, Callable, ExecutorService }
import scalajs.js.timers.setTimeout

class EventLoopExecutor extends ExecutorServiceDelegate {
  private[this] var _isShutdown = false
  
  def execute(command: Runnable) = if (!_isShutdown) setTimeout(0) {
    command.run()
  } 
  
  def shutdown() = _isShutdown = true
  
  def shutdownNow() = ???
  
  def isShutdown() = _isShutdown

  def isTerminated = ???

  def awaitTermination(l: Long, timeUnit: TimeUnit) = ???

  def submit[T](callable: Callable[T]) = ???

  def submit[T](runnable: Runnable, t: T) = ???

  def submit(runnable: Runnable) = ???

  def invokeAll[T](callables: Collection[_ <: Callable[T]]) = ???

  def invokeAll[T](callables: Collection[_ <: Callable[T]], l: Long, timeUnit: TimeUnit) = ???

  def invokeAny[T](callables: Collection[_ <: Callable[T]]) = ???

  def invokeAny[T](callables: Collection[_ <: Callable[T]], l: Long, timeUnit: TimeUnit) = ??? 
}