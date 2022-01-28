package java.util.concurrent

import java.util

abstract class AbstractExecutorService extends ExecutorService {
  def invokeAll[T](tasks: util.Collection[_ <: Callable[T]]): util.List[Future[T]] = ???
  def invokeAll[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): util.List[Future[T]] = ???
  def invokeAny[T](tasks: util.Collection[_ <: Callable[T]]): T = ???
  def invokeAny[T](tasks: util.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): T = ???
  def submit[T](task: Callable[T]): Future[T] = ???
  def submit(task: Runnable): Future[_] = ???
  def submit[T](task: Runnable, result: T): Future[T] = ???
}
