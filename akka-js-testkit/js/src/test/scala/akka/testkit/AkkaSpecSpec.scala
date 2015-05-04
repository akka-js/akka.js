/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.testkit

import language.reflectiveCalls
import language.postfixOps

import org.scalatest.WordSpec
import org.scalatest.Matchers
import akka.actor._
// @note IMPLEMENT IN SCALA.JS import com.typesafe.config.ConfigFactory 
// @note IMPLEMENT IN SCALA.JS import scala.concurrent.Await
import akka.concurrent.Await
import scala.concurrent.duration._
import akka.actor.DeadLetter
import akka.pattern.ask

// @note IMPLEMENT IN SCALA.JS @org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class AkkaSpecSpec extends WordSpec with Matchers {

  "An AkkaSpec" must {

    "warn about unhandled messages" in {
      akka.concurrent.BlockingEventLoop.switch
      implicit val system = ActorSystem("AkkaSpec0", AkkaSpec.testConf)
      try {
        val a = system.actorOf(Props.empty)
        EventFilter.warning(start = "unhandled message", occurrences = 1) intercept {
          a ! 42
        }
      } finally {
        TestKit.shutdownActorSystem(system)
      }
      akka.concurrent.BlockingEventLoop.switch
    }
    /**
    "terminate all actors" in {
      // verbose config just for demonstration purposes, please leave in in case of debugging
      import scala.collection.JavaConverters._
      val conf = Map(
        "akka.actor.debug.lifecycle" -> true, "akka.actor.debug.event-stream" -> true,
        "akka.loglevel" -> "DEBUG", "akka.stdout-loglevel" -> "DEBUG")
      val system = ActorSystem("AkkaSpec1")// @note IMPLEMENT IN SCALA.JS , AkkaSpec.testConf), ConfigFactory.parseMap(conf.asJava).withFallback(AkkaSpec.testConf))
      val spec = new AkkaSpec(system) {
        val ref = Seq(testActor, system.actorOf(Props.empty, "name"))
      }
      spec.ref foreach (_.isTerminated should not be true)
      TestKit.shutdownActorSystem(system)
      spec.awaitCond(spec.ref forall (_.isTerminated), 2 seconds)
    }

    "stop correctly when sending PoisonPill to rootGuardian" in {
      val system = ActorSystem("AkkaSpec2") // @note IMPLEMENT IN SCALA.JS , AkkaSpec.testConf), AkkaSpec.testConf)
      //val spec = new AkkaSpec(system) {}
      val latch = new TestLatch(1)(system)
      system.registerOnTermination(latch.countDown())

      system.actorSelection("/") ! PoisonPill

      // @note IMPLEMENT IN SCALA.JS Await.ready(latch, 2 seconds)
      latch.await
    }

    "enqueue unread messages from testActor to deadLetters" in {
      val system, otherSystem = ActorSystem("AkkaSpec3") // @note IMPLEMENT IN SCALA.JS , AkkaSpec.testConf), AkkaSpec.testConf)

      try {
        var locker = Seq.empty[DeadLetter]
        implicit val timeout = TestKitExtension(system).DefaultTimeout
        val davyJones = otherSystem.actorOf(Props(new Actor {
          def receive = {
            case m: DeadLetter ⇒ locker :+= m
            case "Die!"        ⇒ sender() ! "finally gone"; context.stop(self)
          }
        }), "davyJones")

        system.eventStream.subscribe(davyJones, classOf[DeadLetter])

        val probe = new TestProbe(system)
        probe.ref.tell(42, davyJones)
        /*
       * this will ensure that the message is actually received, otherwise it
       * may happen that the system.stop() suspends the testActor before it had
       * a chance to put the message into its private queue
       */
        probe.receiveWhile(1 second) {
          case null ⇒
        }

        val latch = new TestLatch(1)(system)
        system.registerOnTermination(latch.countDown())
        TestKit.shutdownActorSystem(system)
        // @note IMPLEMENT IN SCALA.JS Await.ready(latch, 2 seconds)
        latch.await
        Await.result(davyJones ? "Die!"/** @note IMPLEMENT IN SCALA.JS , timeout.duration */) should be("finally gone")

        // this will typically also contain log messages which were sent after the logger shutdown
        locker should contain(DeadLetter(42, davyJones, probe.ref))
      } finally {
        TestKit.shutdownActorSystem(system)
        TestKit.shutdownActorSystem(otherSystem)
      }
    }*/
  }
}
