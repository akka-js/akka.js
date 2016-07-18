/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.testkit

import org.scalactic.ConversionCheckedTripleEquals
import org.scalatest.concurrent.ScalaFutures

import language.{ postfixOps, reflectiveCalls }

import org.scalatest.{ WordSpecLike, BeforeAndAfterAll }
import org.scalatest.Matchers
import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import scala.concurrent.duration._
import scala.concurrent.Future
import com.typesafe.config.{ Config, ConfigFactory }
import akka.dispatch.Dispatchers
import akka.testkit.TestEvent._


object AkkaConfig {

  val default = """
akka {
  home = ""
  version = "2.4-SNAPSHOT"
  time-unit = "seconds"
  #loggers = ["akka.event.Logging$DefaultLogger"]
  loggers = ["akka.event.JSDefaultLogger"]
  #loggers = ["akka.event.DefaultLogger"]
  #loggers = ["akka.event.LoggingBusActor"]
  logging-filter = "akka.event.JSDefaultLoggingFilter"
  #logging-filter = "akka.event.DefaultLoggingFilter"
  loggers-dispatcher = "akka.actor.default-dispatcher"
  logger-startup-timeout = 5s
  loglevel = "DEBUG"
  stdout-loglevel = "DEBUG"
  log-config-on-start = off
  log-dead-letters = 0
  log-dead-letters-during-shutdown = off
  library-extensions = []
  extensions = []
  daemonic = off
  jvm-exit-on-fatal-error = on


  stream {

    # Default flow materializer settings
    materializer {

      # Initial size of buffers used in stream elements
      initial-input-buffer-size = 4
      # Maximum size of buffers used in stream elements
      max-input-buffer-size = 16

      # Fully qualified config path which holds the dispatcher configuration
      # to be used by FlowMaterialiser when creating Actors.
      # When this value is left empty, the default-dispatcher will be used.
      dispatcher = ""

      # Cleanup leaked publishers and subscribers when they are not used within a given
      # deadline
      subscription-timeout {
        # when the subscription timeout is reached one of the following strategies on
        # the "stale" publisher:
        # cancel - cancel it (via `onError` or subscribing to the publisher and
        #          `cancel()`ing the subscription right away
        # warn   - log a warning statement about the stale element (then drop the
        #          reference to it)
        # noop   - do nothing (not recommended)
        mode = cancel

        # time after which a subscriber / publisher is considered stale and eligible
        # for cancelation (see `akka.stream.subscription-timeout.mode`)
        timeout = 5s
      }

      # Enable additional troubleshooting logging at DEBUG log level
      debug-logging = on

      # Maximum number of elements emitted in batch if downstream signals large demand
      output-burst-limit = 1000

      # Enable automatic fusing of all graphs that are run. For short-lived streams
      # this may cause an initial runtime overhead, but most of the time fusing is
      # desirable since it reduces the number of Actors that are created.
      auto-fusing = on

      # Those stream elements which have explicit buffers (like mapAsync, mapAsyncUnordered,
      # buffer, flatMapMerge, Source.actorRef, Source.queue, etc.) will preallocate a fixed
      # buffer upon stream materialization if the requested buffer size is less than this
      # configuration parameter. The default is very high because failing early is better
      # than failing under load.
      #
      # Buffers sized larger than this will dynamically grow/shrink and consume more memory
      # per element than the fixed size buffers.
      max-fixed-buffer-size = 1000000000

      # Maximum number of sync messages that actor can process for stream to substream communication.
      # Parameter allows to interrupt synchronous processing to get upsteam/downstream messages.
      # Allows to accelerate message processing that happening withing same actor but keep system responsive.
      sync-processing-limit = 1000

      debug {
        # Enables the fuzzing mode which increases the chance of race conditions
        # by aggressively reordering events and making certain operations more
        # concurrent than usual.
        # This setting is for testing purposes, NEVER enable this in a production
        # environment!
        # To get the best results, try combining this setting with a throughput
        # of 1 on the corresponding dispatchers.
        fuzzing-mode = off
      }
    }

    # Fully qualified config path which holds the dispatcher configuration
    # to be used by FlowMaterialiser when creating Actors for IO operations,
    # such as FileSource, FileSink and others.
    blocking-io-dispatcher = "akka.stream.default-blocking-io-dispatcher"

    default-blocking-io-dispatcher {
      type = "Dispatcher"
      executor = "thread-pool-executor"
      throughput = 1

      thread-pool-executor {
        core-pool-size-min = 2
        core-pool-size-factor = 2.0
        core-pool-size-max = 16
      }
    }
  }


  actor {
    #provider = "akka.actor.LocalActorRefProvider"
    provider = "akka.actor.JSLocalActorRefProvider"
    guardian-supervisor-strategy = "akka.actor.DefaultSupervisorStrategy"
    creation-timeout = 20
    timeout = 5
    serialize-messages = off
    serialize-creators = off
    unstarted-push-timeout = 10
    typed {
      # Default timeout for typed actor methods with non-void return type
      timeout = n
    }
    router.type-mapping {
      from-code = "akka.routing.NoRouter"
      round-robin-pool = "akka.routing.RoundRobinPool"
      round-robin-group = "akka.routing.RoundRobinGroup"
      random-pool = "akka.routing.RandomPool"
      random-group = "akka.routing.RandomGroup"
      balancing-pool = "akka.routing.BalancingPool"
      smallest-mailbox-pool = "akka.routing.SmallestMailboxPool"
      broadcast-pool = "akka.routing.BroadcastPool"
      broadcast-group = "akka.routing.BroadcastGroup"
      scatter-gather-pool = "akka.routing.ScatterGatherFirstCompletedPool"
      scatter-gather-group = "akka.routing.ScatterGatherFirstCompletedGroup"
      tail-chopping-pool = "akka.routing.TailChoppingPool"
      tail-chopping-group = "akka.routing.TailChoppingGroup"
      consistent-hashing-pool = "akka.routing.ConsistentHashingPool"
      consistent-hashing-group = "akka.routing.ConsistentHashingGroup"
    }
    deployment {
      default {
        dispatcher = ""
        mailbox = ""
        router = "from-code"
        nr-of-instances = 1
        within = 5 seconds
        virtual-nodes-factor = 10
        tail-chopping-router {
          interval = 10 milliseconds
        }
        routees {
          paths = []
        }
        resizer {
          enabled = off
          lower-bound = 1
          upper-bound = 10
          pressure-threshold = 1
          rampup-rate = 0.2
          backoff-threshold = 0.3
          backoff-rate = 0.1
          messages-per-resize = 10
        }
        optimal-size-exploring-resizer {
          enabled = off
          lower-bound = 1
          chance-of-ramping-down-when-full = 0.2
          action-interval = 5s
          downsize-after-underutilized-for = 72h
          explore-step-size = 0.1
          chance-of-exploration = 0.4
          downsize-ratio = 0.8
          optimization-range = 16
          weight-of-latest-metric = 0.5
        }
      }
    }

    default-dispatcher {
      type = "Dispatcher"
      executor = "default-executor"
      default-executor {
        fallback = "fork-join-executor"
      }
      fork-join-executor {
        parallelism-min = 8
        parallelism-factor = 3.0
        parallelism-max = 64
        task-peeking-mode = "FIFO"
      }
      thread-pool-executor {
        keep-alive-time = 60s
        fixed-pool-size = off
        core-pool-size-min = 8
        core-pool-size-factor = 3.0
        core-pool-size-max = 64
        max-pool-size-min = 8
        max-pool-size-factor  = 3.0
        max-pool-size-max = 64
        task-queue-size = -1
        task-queue-type = "linked"
        allow-core-timeout = on
      }
      shutdown-timeout = 1s
      throughput = 5
      throughput-deadline-time = 0ms
      attempt-teamwork = on
      mailbox-requirement = ""
    }

    default-mailbox {
      mailbox-type = "akka.dispatch.UnboundedMailbox"
      mailbox-capacity = 1000
      mailbox-push-timeout-time = 10s
      stash-capacity = -1
    }
    mailbox {
      requirements {
        "akka.dispatch.UnboundedMessageQueueSemantics" =
          akka.actor.mailbox.unbounded-queue-based
        "akka.dispatch.BoundedMessageQueueSemantics" =
          akka.actor.mailbox.bounded-queue-based
        "akka.dispatch.DequeBasedMessageQueueSemantics" =
          akka.actor.mailbox.unbounded-deque-based
        "akka.dispatch.UnboundedDequeBasedMessageQueueSemantics" =
          akka.actor.mailbox.unbounded-deque-based
        "akka.dispatch.BoundedDequeBasedMessageQueueSemantics" =
          akka.actor.mailbox.bounded-deque-based
        "akka.dispatch.MultipleConsumerSemantics" =
          akka.actor.mailbox.unbounded-queue-based
        "akka.dispatch.ControlAwareMessageQueueSemantics" =
          akka.actor.mailbox.unbounded-control-aware-queue-based
        "akka.dispatch.UnboundedControlAwareMessageQueueSemantics" =
          akka.actor.mailbox.unbounded-control-aware-queue-based
        "akka.dispatch.BoundedControlAwareMessageQueueSemantics" =
          akka.actor.mailbox.bounded-control-aware-queue-based
        "akka.event.LoggerMessageQueueSemantics" =
          akka.actor.mailbox.logger-queue
      }
      unbounded-queue-based {
        mailbox-type = "akka.dispatch.UnboundedMailbox"
      }
      bounded-queue-based {
        mailbox-type = "akka.dispatch.BoundedMailbox"
      }
      unbounded-deque-based {
        mailbox-type = "akka.dispatch.UnboundedDequeBasedMailbox"
      }
      bounded-deque-based {
        mailbox-type = "akka.dispatch.BoundedDequeBasedMailbox"
      }
      unbounded-control-aware-queue-based {
        mailbox-type = "akka.dispatch.UnboundedControlAwareMailbox"
      }
      bounded-control-aware-queue-based {
        mailbox-type = "akka.dispatch.BoundedControlAwareMailbox"
      }
      logger-queue {
        mailbox-type = "akka.event.LoggerMailboxType"
      }
    }
    debug {
      receive = on
      autoreceive = on
      lifecycle = on
      fsm = on
      event-stream = on
      unhandled = on
      router-misconfiguration = on
    }
    serializers {
      java = "akka.serialization.JavaSerializer"
      bytes = "akka.serialization.ByteArraySerializer"
    }
    serialization-bindings {
      "[B" = bytes
      "java.io.Serializable" = java
    }
    warn-about-java-serializer-usage = on
    serialization-identifiers {
      "akka.serialization.JavaSerializer" = 1
      "akka.serialization.ByteArraySerializer" = 4
    }
    dsl {
      inbox-size = 1000
      default-timeout = 5s
    }
  }
  scheduler {
    tick-duration = 10ms
    ticks-per-wheel = 512
    #implementation = akka.actor.LightArrayRevolverScheduler
    implementation = akka.actor.EventLoopScheduler
    shutdown-timeout = 5s
  }
}
                """

}


object AkkaSpec {



  akka.actor.JSDynamicAccess.injectClass(
    "akka.testkit.TestEventListener" -> classOf[akka.testkit.TestEventListener]
  )


  def testConf(configMap: Map[String, _]) : Config =
    ConfigFactory.parseString(configMap.map {case (key, value) => s"$key=$value"}.mkString("\n"))
  def testConf(s: String) : Config = ConfigFactory.parseString(s).withFallback(AkkaSpec.testConf)
  def testConf: Config = ConfigFactory.parseString(AkkaConfig.default)
/*
  def mapToConfig(map: Map[String, Any]): Config = {
    import scala.collection.JavaConverters._
    ConfigFactory.parseMap(map.asJava)
  }*/

  /*def getCallerName(clazz: Class[_]): String = {
    val s = (Thread.currentThread.getStackTrace map (_.getClassName) drop 1)
      .dropWhile(_ matches "(java.lang.Thread|.*AkkaSpec.?$)")
    val reduced = s.lastIndexWhere(_ == clazz.getName) match {
      case -1 ⇒ s
      case z  ⇒ s drop (z + 1)
    }
    reduced.head.replaceFirst(""".*\.""", "").replaceAll("[^a-zA-Z_0-9]", "_")
  }*/

}

abstract class AkkaSpec(_system: ActorSystem)
// @todo Check ScalaFutures
  extends TestKit(_system) with WordSpecLike with Matchers with BeforeAndAfterAll with ScalaFutures{ // @note IMPLEMENT IN SCALA.JS with WatchedByCoroner {

  /** @note IMPLEMENT IN SCALA.JS
  def this(config: Config) = this(ActorSystem(AkkaSpec.getCallerName(getClass),
    ConfigFactory.load(AkkaSpec.testConf)))

  def this(s: String) = this(ConfigFactory.parseString(s))

  def this(configMap: Map[String, _]) = this(AkkaSpec.mapToConfig(configMap))

  def this() = this(ActorSystem(AkkaSpec.getCallerName(getClass), AkkaSpec.testConf))
  */

  //@todo make right

  def this(configMap: Map[String, _]) =
    this(ActorSystem(scala.util.Random.alphanumeric.take(10).mkString,
      AkkaSpec.testConf(configMap)))

  def this(config: Config) = this(ActorSystem(scala.util.Random.alphanumeric.take(10).mkString,
    config))

  def this(s: String) = this(ActorSystem(scala.util.Random.alphanumeric.take(10).mkString, AkkaSpec.testConf(s)))

  def this() = this(ActorSystem(scala.util.Random.alphanumeric.take(10).mkString, AkkaSpec.testConf))

  val log: LoggingAdapter = Logging(system, this.getClass)

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected = true

  final override def beforeAll {
    // @note IMPLEMENT IN SCALA.JS startCoroner
    atStartup()
  }

  final override def afterAll {
    beforeTermination()
    shutdown()
    afterTermination()
    // @note IMPLEMENT IN SCALA.JS stopCoroner()
  }

  protected def atStartup() {}

  protected def beforeTermination() {}

  protected def afterTermination() {}

  def spawn(dispatcherId: String = Dispatchers.DefaultDispatcherId)(body: ⇒ Unit): Unit =
    Future(body)(system.dispatchers.lookup(dispatcherId))

  /** @note IMPLEMENT IN SCALA.JS override */ def expectedTestDuration: FiniteDuration = 60 seconds

  def muteDeadLetters(messageClasses: Class[_]*)(sys: ActorSystem = system): Unit = ()
    /** @note IMPLEMENT IN SCALA.JS
    if (!sys.log.isDebugEnabled) {
      def mute(clazz: Class[_]): Unit =
        sys.eventStream.publish(Mute(DeadLettersFilter(clazz)(occurrences = Int.MaxValue)))
      if (messageClasses.isEmpty) mute(classOf[AnyRef])
      else messageClasses foreach mute
    }
    */

}
