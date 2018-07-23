package akka.stream.typed.scaladsl

import scala.concurrent.Future
import scala.concurrent.duration.Duration

object Await {

  def ready[T](f: Future[T], atMost: Duration): f.type =
    akka.testkit.Await.ready(f, atMost)

  def ready[T](awaitable: akka.testkit.Awaitable[T], atMost: Duration): awaitable.type =
    akka.testkit.Await.ready(awaitable, atMost)

  def result[T](f: Future[T]): T =
    akka.testkit.Await.result(f)

  def result[T](f: Future[T], atMost: Duration): T =
    akka.testkit.Await.result(f, atMost)

  def result[T](awaitable: akka.testkit.Awaitable[T], atMost: Duration): T =
    akka.testkit.Await.result(awaitable, atMost)

}
