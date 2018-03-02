/**
 * Copyright (C) 2017-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package akka.testkit.typed

import com.typesafe.config.Config
import scala.concurrent.duration.FiniteDuration
import akka.util.Timeout
import akka.actor.typed.ActorSystem

object TestKitSettings {
  /**
   * Reads configuration settings from `akka.actor.typed.test` section.
   */
  def apply(system: ActorSystem[_]): TestKitSettings =
    apply(system.settings.config)

  /**
   * Reads configuration settings from given `Config` that
   * must have the same layout as the `akka.actor.typed.test` section.
   */
  def apply(config: Config): TestKitSettings =
    new TestKitSettings(config)

  /**
   * Java API: Reads configuration settings from `akka.actor.typed.test` section.
   */
  def create(system: ActorSystem[_]): TestKitSettings =
    apply(system)

  /**
   * Reads configuration settings from given `Config` that
   * must have the same layout as the `akka.actor.typed.test` section.
   */
  def create(config: Config): TestKitSettings =
    new TestKitSettings(config)
}

class TestKitSettings(val config: Config) {

  import akka.util.Helpers._

  // val TestTimeFactor = config.getDouble("akka.actor.typed.test.timefactor").
  //   requiring(tf ⇒ !tf.isInfinite && tf > 0, "akka.actor.typed.test.timefactor must be positive finite double")
  // val SingleExpectDefaultTimeout: FiniteDuration = config.getMillisDuration("akka.actor.typed.test.single-expect-default")
  // val DefaultTimeout: Timeout = Timeout(config.getMillisDuration("akka.actor.typed.test.default-timeout"))
  import scala.concurrent.duration._
  val TestTimeFactor = 3.0
  val SingleExpectDefaultTimeout: FiniteDuration = 5 seconds
  val DefaultTimeout: Timeout = Timeout(8 seconds)
  val ExpectNoMessageDefaultTimeout = 500 millis
  val DefaultActorSystemShutdownTimeout: FiniteDuration = 3 seconds
  val ThrowOnShutdownTimeout: Boolean = false

  def dilated(duration: FiniteDuration): FiniteDuration = (duration * TestTimeFactor).asInstanceOf[FiniteDuration]
}
