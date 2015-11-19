/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.testkit

import akka.actor.{ Props, Actor }

/**
 * A collection of common actor patterns used in tests.
 */
@scala.scalajs.js.annotation.JSExport
class EchoActor extends Actor {
  override def receive = {
    case message ⇒ sender() ! message
  }
}

object TestActors {

  /**
   * EchoActor sends back received messages (unmodified).
   */

  val echoActorProps = Props[EchoActor]()

}
