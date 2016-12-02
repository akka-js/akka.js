/**
 * Copyright (C) 2009-2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.testkit

import com.typesafe.config.{Config, ConfigFactory}
import akka.util.Timeout
import akka.actor.{ ExtensionId, ActorSystem, Extension, ExtendedActorSystem }
import scala.concurrent.duration.FiniteDuration

object TestKitExtension extends ExtensionId[TestKitSettings] {
  //should make it right!
  val defaultTestConfig = """
akka{
  test {
    # factor by which to scale timeouts during tests, e.g. to account for shared
    # build system load
    timefactor =  3.0

    # duration of EventFilter.intercept waits after the block is finished until
    # all required messages are received
    filter-leeway = 3s

    # duration to wait in expectMsg and friends outside of within() block
    # by default
    single-expect-default = 3s

    # The timeout that is added as an implicit by DefaultTimeout trait
    default-timeout = 5s

    calling-thread-dispatcher {
      type = akka.testkit.CallingThreadDispatcherConfigurator
    }
  }
}
  """

  override def get(system: ActorSystem): TestKitSettings = super.get(system)
  def createExtension(system: ExtendedActorSystem): TestKitSettings = {
    new TestKitSettings(system.settings.config.withFallback(ConfigFactory.parseString(defaultTestConfig)))
  }

  override def apply(system: ActorSystem): TestKitSettings = {
    createExtension(system.asInstanceOf[ExtendedActorSystem])
  }
}

class TestKitSettings(val config: Config) extends Extension {

  import akka.util.Helpers._

  val TestTimeFactor = config.getDouble("akka.test.timefactor").
    requiring(tf ⇒ !tf.isInfinite && tf > 0, "akka.test.timefactor must be positive finite double")
  val SingleExpectDefaultTimeout: FiniteDuration = config.getMillisDuration("akka.test.single-expect-default")
  val TestEventFilterLeeway: FiniteDuration = config.getMillisDuration("akka.test.filter-leeway")
  val DefaultTimeout: Timeout = Timeout(config.getMillisDuration("akka.test.default-timeout"))
}
