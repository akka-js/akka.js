/*
 * Copyright (C) 2017-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.actor.testkit.typed

import com.typesafe.config.Config
import scala.concurrent.duration.{ Duration, FiniteDuration }

import akka.util.JavaDurationConverters._
import akka.util.Timeout
import akka.actor.typed.ActorSystem
import akka.actor.typed.Extension
import akka.actor.typed.ExtensionId

object TestKitSettings {

  /**
   * Reads configuration settings from `akka.actor.testkit.typed` section.
   */
  def apply(system: ActorSystem[_]): TestKitSettings =
    Ext(system).settings

  /**
   * Reads configuration settings from given `Config` that
   * must have the same layout as the `akka.actor.testkit.typed` section.
   */
  def apply(config: Config): TestKitSettings =
    new TestKitSettings(config)

  /**
   * Java API: Reads configuration settings from `akka.actor.testkit.typed` section.
   */
  def create(system: ActorSystem[_]): TestKitSettings =
    apply(system)

  /**
   * Reads configuration settings from given `Config` that
   * must have the same layout as the `akka.actor.testkit.typed` section.
   */
  def create(config: Config): TestKitSettings =
    new TestKitSettings(config)

  private object Ext extends ExtensionId[Ext] {
    override def createExtension(system: ActorSystem[_]): Ext = new Ext(system)
    def get(system: ActorSystem[_]): Ext = apply(system)
  }

  private class Ext(system: ActorSystem[_]) extends Extension {
    val settings: TestKitSettings = TestKitSettings(system.settings.config.getConfig("akka.actor.testkit.typed"))
  }
}

final class TestKitSettings(val config: Config) {

  import akka.util.Helpers._

  import scala.concurrent.duration._
  val TestTimeFactor = 3.0
  val SingleExpectDefaultTimeout: FiniteDuration = 5 seconds
  val DefaultTimeout: Timeout = Timeout(8 seconds)
  val ExpectNoMessageDefaultTimeout = 500 millis
  val DefaultActorSystemShutdownTimeout: FiniteDuration = 3 seconds
  val ThrowOnShutdownTimeout: Boolean = false
  val FilterLeeway: FiniteDuration = 2 seconds

  /**
   * Scala API: Scale the `duration` with the configured `TestTimeFactor`
   */
  def dilated(duration: FiniteDuration): FiniteDuration =
    Duration.fromNanos((duration.toNanos * TestTimeFactor + 0.5).toLong)

  /**
   * Java API: Scale the `duration` with the configured `TestTimeFactor`
   */
  def dilated(duration: java.time.Duration): java.time.Duration =
    dilated(duration.asScala).asJava
}
