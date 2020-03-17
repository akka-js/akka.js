package java.util.concurrent

import java.{util => jul}

trait ExecutorService extends Executor {
  def awaitTermination(timeout: Long, unit: TimeUnit): Boolean
  def invokeAll[T](tasks: jul.Collection[_ <: Callable[T]]): jul.List[Future[T]]
  def invokeAll[T](tasks: jul.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): jul.List[Future[T]]
  def invokeAny[T](tasks: jul.Collection[_ <: Callable[T]]): T
  def invokeAny[T](tasks: jul.Collection[_ <: Callable[T]], timeout: Long, unit: TimeUnit): T
  def	isShutdown(): Boolean
  def	isTerminated(): Boolean
  def shutdown(): Unit
  def shutdownNow(): List[Runnable]
  def submit[T](task: Callable[T]): Future[T]
  def submit(task: Runnable): Future[_]
  def submit[T](task: Runnable, result: T): Future[T]
}
