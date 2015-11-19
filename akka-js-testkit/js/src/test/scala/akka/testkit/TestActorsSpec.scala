/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.testkit

import akka.concurrent.BlockingEventLoop

class TestActorsSpec extends AkkaSpec with ImplicitSender {

  import TestActors.echoActorProps

  "A EchoActor" must {
    "send back messages unchanged" in {
      BlockingEventLoop.switch
      val message = "hello world"
      val echo = system.actorOf(echoActorProps)

      echo ! message

      expectMsg(message)
      BlockingEventLoop.reset
    }
  }
}
