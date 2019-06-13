package akka.actor.testkit.typed.scaladsl

import scala.concurrent.duration._
// fixing the hard way

object Promise {

  def apply[T]() = new WrappedPromise[T]{
    val inner = scala.concurrent.Promise[T]()
  }

}

trait WrappedPromise[T] extends scala.concurrent.Promise[T] {
  self =>
  val inner: scala.concurrent.Promise[T] 

  lazy val future: WrappedFuture[T] = new WrappedFuture[T]{
    val prom = self.inner
  }

  def isCompleted: Boolean =
    inner.isCompleted
  def tryComplete(result: scala.util.Try[T]): Boolean =
    inner.tryComplete(result)
}

trait WrappedFuture[T] extends scala.concurrent.Future[T] {
  self =>
  val prom: scala.concurrent.Promise[T]
  lazy val inner = prom.future

  def futureValue(): T =
    akka.testkit.Await.result(inner, 5 seconds) // make it configurable? .asInstanceOf[T]

  def ready(atMost: scala.concurrent.duration.Duration)(implicit permit: scala.concurrent.CanAwait): this.type = {
    self.inner.ready(atMost)(permit)
    this
  }
  def result(atMost: scala.concurrent.duration.Duration)(implicit permit: scala.concurrent.CanAwait): T =
    inner.result(atMost)(permit)

  def isCompleted: Boolean =
    inner.isCompleted
  def onComplete[U](f: scala.util.Try[T] => U)(implicit executor: scala.concurrent.ExecutionContext): Unit =
    inner.onComplete[U](f)(executor)
  def value: Option[scala.util.Try[T]] =
    inner.value

}
