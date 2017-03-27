/**
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.actor

object NonPublicClass {
  def createProps(): Props =
    Props(new MyNonPublicActorClass())
}

class MyNonPublicActorClass extends Actor {
  def receive = {
    case msg => sender() ! msg
  }
}
