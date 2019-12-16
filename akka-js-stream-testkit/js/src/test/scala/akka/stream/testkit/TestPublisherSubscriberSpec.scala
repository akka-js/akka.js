/*
 * Copyright (C) 2015-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.testkit

import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.testkit.TestPublisher._
import akka.stream.testkit.TestSubscriber._
import akka.stream.testkit.scaladsl.StreamTestKit._
import akka.testkit.AkkaSpec
import org.reactivestreams.Subscription

// Scala.Js
// There is a bug in Shocon to be fixed (not merging properly objects)
class TestPublisherSubscriberSpec extends AkkaSpec("""
    akka.stream.materializer {
      creation-timeout = 20s
      initial-input-buffer-size = 2
      max-input-buffer-size = 2
      blocking-io-dispatcher = "akka.stream.default-blocking-io-dispatcher"
      dispatcher = ""
      subscription-timeout {
        mode = cancel
        timeout = 5s
      }
      debug-logging = off
      output-burst-limit = 1000
      auto-fusing = on
      max-fixed-buffer-size = 1000000000
      sync-processing-limit = 1000
      debug {
        fuzzing-mode = off
      }
      io {
        tcp {
          write-buffer-size = 16 KiB
        }
      }
      stream-ref {
        buffer-capacity = 32
        demand-redelivery-interval = 1 second
        subscription-timeout = 30 seconds
        final-termination-signal-deadline = 2 seconds
      }
    }
  """) {

  "TestPublisher and TestSubscriber" must {

    "have all events accessible from manual probes" in assertAllStagesStopped {
      val upstream = TestPublisher.manualProbe[Int]()
      val downstream = TestSubscriber.manualProbe[Int]()
      Source.fromPublisher(upstream).runWith(Sink.asPublisher(false)).subscribe(downstream)

      val upstreamSubscription = upstream.expectSubscription()
      val downstreamSubscription: Subscription = downstream.expectEventPF { case OnSubscribe(sub) => sub }

      upstreamSubscription.sendNext(1)
      downstreamSubscription.request(1)
      upstream.expectEventPF { case RequestMore(_, e) => e } should ===(1L)
      downstream.expectEventPF { case OnNext(e)       => e } should ===(1)

      upstreamSubscription.sendNext(1)
      downstreamSubscription.request(1)
      downstream.expectNextPF[Int] { case e: Int => e } should ===(1)

      upstreamSubscription.sendComplete()
      downstream.expectEventPF {
        case OnComplete =>
        case _          => fail()
      }
    }

    "handle gracefully partial function that is not suitable" in assertAllStagesStopped {
      val upstream = TestPublisher.manualProbe[Int]()
      val downstream = TestSubscriber.manualProbe[Int]()
      Source.fromPublisher(upstream).runWith(Sink.asPublisher(false)).subscribe(downstream)
      val upstreamSubscription = upstream.expectSubscription()
      val downstreamSubscription: Subscription = downstream.expectEventPF { case OnSubscribe(sub) => sub }

      upstreamSubscription.sendNext(1)
      downstreamSubscription.request(1)
      an[AssertionError] should be thrownBy upstream.expectEventPF { case Subscribe(e)       => e }
      an[AssertionError] should be thrownBy downstream.expectNextPF[String] { case e: String => e }

      upstreamSubscription.sendComplete()
    }

    "properly update pendingRequest in expectRequest" in {
      val upstream = TestPublisher.probe[Int]()
      val downstream = TestSubscriber.manualProbe[Int]()

      Source.fromPublisher(upstream).runWith(Sink.fromSubscriber(downstream))

      downstream.expectSubscription().request(10)

      upstream.expectRequest() should ===(10L)
      upstream.sendNext(1)
      downstream.expectNext(1)
    }

  }
}
