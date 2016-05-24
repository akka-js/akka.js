package akka.event

import scala.concurrent.Awaitable
import scala.concurrent.duration.Duration

object Await {
  //This is a dirty Hack I know....
  def result[T](awaitable: => Awaitable[T], atMost: Duration) =
    Logging.LoggerInitialized.asInstanceOf[T]
}
