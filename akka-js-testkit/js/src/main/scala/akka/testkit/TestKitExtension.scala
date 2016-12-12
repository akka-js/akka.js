/**
 * Copyright (C) 2009-2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.testkit

import com.typesafe.config.Config
import akka.util.Timeout
import akka.actor.{ ExtensionId, ActorSystem, Extension, ExtendedActorSystem }
import scala.concurrent.duration.FiniteDuration

object TestKitExtension extends ExtensionId[TestKitSettings] {
  override def get(system: ActorSystem): TestKitSettings = super.get(system)
  def createExtension(system: ExtendedActorSystem): TestKitSettings = new TestKitSettings(system.settings.config)

  //hack to keep things working
  override def apply(system: ActorSystem) = createExtension(system.asInstanceOf[ExtendedActorSystem])
}

class TestKitSettings(val _config: Config) extends Extension {

  //There is a bug around here :-S
  val config = com.typesafe.config.ConfigFactory.load()

  import akka.util.Helpers._

  val TestTimeFactor = config.getDouble("akka.test.timefactor").
    requiring(tf ⇒ !tf.isInfinite && tf > 0, "akka.test.timefactor must be positive finite double")
  val SingleExpectDefaultTimeout: FiniteDuration = config.getMillisDuration("akka.test.single-expect-default")
  val TestEventFilterLeeway: FiniteDuration = config.getMillisDuration("akka.test.filter-leeway")
  val DefaultTimeout: Timeout = Timeout(config.getMillisDuration("akka.test.default-timeout"))
}
