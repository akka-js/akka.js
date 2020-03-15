/**
 * Copyright (C) 2009-2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.testkit

import language.postfixOps

import org.scalatest.WordSpec
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.actor._
import com.typesafe.config.ConfigFactory
import scala.concurrent.Promise
import akka.testkit.Await
import scala.concurrent.duration._
import akka.actor.DeadLetter
import akka.pattern.ask

class ManagedEventLoopSpec extends WordSpec with Matchers with BeforeAndAfterAll {

  override def beforeAll() = {
    ManagedEventLoop.manage
  }

  override def afterAll() = {
    ManagedEventLoop.reset
  }

  "The ManagedEvetLoop" must {

    "propertly perform Await operations" in {

      val timeout = 1 second
      val p = Promise[Boolean]

      val system = ActorSystem()

      import system.dispatcher
      system.scheduler.scheduleOnce(timeout){
        p.success(true)
      }

      Await.result(p.future, timeout * 2) should be(true)

      system.terminate
      Await.result(system.whenTerminated, 10 seconds)
    }

    "await multiple results" in {

      val timeout = 1 second
      val p1 = Promise[Boolean]
      val p2 = Promise[Boolean]

      val system = ActorSystem()

      import system.dispatcher
      system.scheduler.scheduleOnce(timeout){
        p1.success(true)
        system.scheduler.scheduleOnce(timeout){
          p2.success(true)
        }
      }

      Await.result(p1.future, timeout * 2) should be(true)
      Await.result(p2.future, timeout * 4) should be(true)

      system.terminate
      Await.result(system.whenTerminated, 10 seconds)
    }

    "await multiple results in different order" in {

      val timeout = 1 second
      val p1 = Promise[Boolean]
      val p2 = Promise[Boolean]

      val system = ActorSystem()

      import system.dispatcher
      system.scheduler.scheduleOnce(timeout){
        p2.success(true)
        system.scheduler.scheduleOnce(timeout){
          p1.success(true)
        }
      }

      Await.result(p1.future, timeout * 4) should be(true)
      Await.result(p2.future, timeout * 4) should be(true)

      system.terminate
      Await.result(system.whenTerminated, 10 seconds)
    }

  }
}
