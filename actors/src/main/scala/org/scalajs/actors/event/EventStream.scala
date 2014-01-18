package org.scalajs.actors.event

class EventStream {
  import Logging._

  def publish(event: LogEvent): Unit = {
    Console.err.println(s"Event: $event")
  }

  def publish(event: Any): Unit = {
    Console.err.println(s"Event: $event")
  }

}
