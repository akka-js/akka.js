/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.testkit

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
  loggers = ["akka.testkit.TestEventListener"]
  logging-filter = "akka.event.JSDefaultLoggingFilter"
  loggers-dispatcher = "akka.actor.default-dispatcher"
  logger-startup-timeout = 5s
  loglevel = "INFO"
  stdout-loglevel = "DEBUG"
  log-config-on-start = off
  log-dead-letters = 0
  log-dead-letters-during-shutdown = off
  actor {
    provider = "akka.actor.JSLocalActorRefProvider"
    guardian-supervisor-strategy = "akka.actor.DefaultSupervisorStrategy"
    debug {
      receive = off
      autoreceive = off
      lifecycle = off
      event-stream = off
      unhandled = off
    }
  }
  scheduler {
    implementation = akka.actor.EventLoopScheduler
  }
}
"""

}


object AkkaSpec {



  akka.actor.JSDynamicAccess.injectClass(
    "akka.testkit.TestEventListener" -> classOf[akka.testkit.TestEventListener]
  )



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
  extends TestKit(_system) with WordSpecLike with Matchers with BeforeAndAfterAll { // @note IMPLEMENT IN SCALA.JS with WatchedByCoroner {

  /** @note IMPLEMENT IN SCALA.JS
  def this(config: Config) = this(ActorSystem(AkkaSpec.getCallerName(getClass),
    ConfigFactory.load(AkkaSpec.testConf)))

  def this(s: String) = this(ConfigFactory.parseString(s))

  def this(configMap: Map[String, _]) = this(AkkaSpec.mapToConfig(configMap))

  def this() = this(ActorSystem(AkkaSpec.getCallerName(getClass), AkkaSpec.testConf))
  */
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
