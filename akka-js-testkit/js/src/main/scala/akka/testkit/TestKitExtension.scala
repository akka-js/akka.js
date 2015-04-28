/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.testkit

import com.typesafe.config.Config
import akka.util.Timeout
import akka.actor.{ ExtensionId, ActorSystem, Extension, ExtendedActorSystem }
import scala.concurrent.duration.{ FiniteDuration, Duration, SECONDS }

object TestKitExtension extends ExtensionId[TestKitSettings] {
  override def get(system: ActorSystem): TestKitSettings = super.get(system)
  def createExtension(system: ExtendedActorSystem): TestKitSettings = new TestKitSettings(new Config /** system.settings.config */)
}

class TestKitSettings(val config: Config) extends Extension {

  import akka.util.Helpers._

  /** @note IMPLEMENT IN SCALA.JS
  val TestTimeFactor = config.getDouble("akka.test.timefactor").
    requiring(tf ⇒ !tf.isInfinite && tf > 0, "akka.test.timefactor must be positive finite double")
  val SingleExpectDefaultTimeout: FiniteDuration = config.getMillisDuration("akka.test.single-expect-default")
  val TestEventFilterLeeway: FiniteDuration = config.getMillisDuration("akka.test.filter-leeway")
  val DefaultTimeout: Timeout = Timeout(config.getMillisDuration("akka.test.default-timeout"))
  */
  val TestTimeFactor = 1.0.
    requiring(tf ⇒ !tf.isInfinite && tf > 0, "akka.test.timefactor must be positive finite double")
  val SingleExpectDefaultTimeout: FiniteDuration = Duration(3, SECONDS)
  val TestEventFilterLeeway: FiniteDuration = Duration(3, SECONDS)
  val DefaultTimeout: Timeout = Timeout(Duration(5, SECONDS))
}
