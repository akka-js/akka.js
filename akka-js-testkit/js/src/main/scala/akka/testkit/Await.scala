package akka.testkit

import scala.concurrent.Future
import scala.concurrent.duration.Duration

import org.scalatest.concurrent.ScalaFutures._

object Await {

  def result[T](f: Future[T], timeout: Duration) = {
    f.isReadyWithin(timeout)
    f.value.get.get
  }

}
