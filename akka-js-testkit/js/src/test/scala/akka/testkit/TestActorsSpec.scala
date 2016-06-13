/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.testkit

import akka.concurrent._

class TestActorsSpec extends AkkaSpec with ImplicitSender {
  ManagedEventLoop.manage
  import TestActors.echoActorProps

  "A EchoActor" must {
    "send back messages unchanged" in {

      val message = "hello world"
      val echo = system.actorOf(echoActorProps)

      import system.dispatcher
      import scala.concurrent.duration._
      system.scheduler.scheduleOnce(0 millis)(
        echo ! message
      )

      expectMsg(message)

    }
  }
}
