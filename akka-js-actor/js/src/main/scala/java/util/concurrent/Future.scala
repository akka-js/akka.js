package java.util.concurrent

trait Future[V] {
  def cancel(mayInterruptIfRunning: Boolean): Boolean
  def get(): V
  def get(timeout: Long, unit: TimeUnit): V
  def isCancelled(): Boolean
  def isDone(): Boolean
}
