package akka.dispatch

import java.util.Collection
import java.util.concurrent.{ TimeUnit, Callable, ExecutorService }
import scalajs.js.timers.setTimeout
import scalajs.js.Dynamic.global

class EventLoopExecutor extends ExecutorServiceDelegate {
  def executor: ExecutorService = this

  private[this] var _isShutdown = false

  // XXX: DO NOT CHANGE THIS TO USE scaaljs.js.timers.setTimeout
  // We need to access global because otherwise the overridden setTimeout
  // in `akka-js-testkit` fails to execute
  override def execute(command: Runnable) =
    if (!_isShutdown) {
      global.setTimeout(command.run _, 0)
      //this fails it means that we are not properly getting things from env?
      //global.setTimeout(command.run, 0)
      /*var fn: scalajs.js.Dynamic = null
      def clear = global.clearInterval(fn)
      fn = global.setInterval({ () =>
        command.run()
        clear
      }, 0)*/
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
