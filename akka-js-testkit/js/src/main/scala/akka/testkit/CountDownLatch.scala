package akka.testkit

import java.util.concurrent.TimeUnit

import scala.concurrent.Promise
import scala.concurrent.duration.Duration

import org.scalatest.concurrent.ScalaFutures._

class CountDownLatch(val c: Int) {

  private var counter = c

  private val closed = Promise[Unit]

  def countDown(): Unit = {
    counter -= 1
    if(counter == 0)
      closed.success(())
  }

  def getCount() =
    counter

  def reset() =
    counter = c

  def await(_timeout: Long, unit: TimeUnit): Boolean = {
    val result = closed.future

    result.isReadyWithin(Duration.fromNanos(TimeUnit.NANOSECONDS.convert(_timeout, unit)))
    result.value
    true
  }
}
