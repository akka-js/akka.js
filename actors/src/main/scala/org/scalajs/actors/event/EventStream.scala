package org.scalajs.actors.event

class EventStream {
  import Logging._

  def publish(event: LogEvent): Unit = {
    Console.err.println(event)
  }

}
