package akka.dispatch

import java.util.Collection
import java.util.concurrent.{ TimeUnit, Callable, ExecutorService }
import scalajs.js.timers.setTimeout

class EventLoopExecutor extends ExecutorServiceDelegate {
  def executor: ExecutorService = this
  
  private[this] var _isShutdown = false
  
  override def execute(command: Runnable) = if (!_isShutdown) setTimeout(0) {
    command.run()
  } 
  
  override def shutdown() = _isShutdown = true
  
  override def shutdownNow() = ???
  
  override def isShutdown() = _isShutdown

  override def isTerminated = ???

  override def awaitTermination(l: Long, timeUnit: TimeUnit) = ???

  override def submit[T](callable: Callable[T]) = ???

  override def submit[T](runnable: Runnable, t: T) = ???

  override def submit(runnable: Runnable) = ???

  override def invokeAll[T](callables: Collection[_ <: Callable[T]]) = ???

  override def invokeAll[T](callables: Collection[_ <: Callable[T]], l: Long, timeUnit: TimeUnit) = ???

  override def invokeAny[T](callables: Collection[_ <: Callable[T]]) = ???

  override def invokeAny[T](callables: Collection[_ <: Callable[T]], l: Long, timeUnit: TimeUnit) = ??? 
}