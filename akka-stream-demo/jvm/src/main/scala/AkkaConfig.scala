package eu.unicredit

import com.typesafe.config.{Config, ConfigFactory}

object AkkaConfig {

  val default = """
akka {
  home = ""
  loggers = ["akka.event.Logging$DefaultLogger"]
  logging-filter = "akka.event.DefaultLoggingFilter"
  loggers-dispatcher = "akka.actor.default-dispatcher"
  logger-startup-timeout = 5s
  loglevel = "INFO"
  stdout-loglevel = "WARNING"
  log-config-on-start = off
  log-dead-letters = 0
  log-dead-letters-during-shutdown = off
  library-extensions = []
  extensions = []
  daemonic = off
  jvm-exit-on-fatal-error = on

  actor {
    provider = "akka.actor.LocalActorRefProvider"
    guardian-supervisor-strategy = "akka.actor.DefaultSupervisorStrategy"
    creation-timeout = 20s
    serialize-messages = off
    serialize-creators = off
    unstarted-push-timeout = 10s
    typed {
      # Default timeout for typed actor methods with non-void return type
      timeout = 5s
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
      receive = off
      autoreceive = off
      lifecycle = off
      fsm = off
      event-stream = off
      unhandled = off
      router-misconfiguration = off
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
    implementation = akka.actor.LightArrayRevolverScheduler
    shutdown-timeout = 5s
  }
}
"""

  import com.typesafe.config.{ Config, ConfigFactory }

  val config: Config = ConfigFactory.parseString(default)

}
