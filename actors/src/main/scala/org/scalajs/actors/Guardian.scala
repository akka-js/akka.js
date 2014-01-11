package org.scalajs.actors

class Guardian extends Actor {
  def receive = {
    case msg =>
      Console.err.println(s"guardian received message $msg")
  }
}
