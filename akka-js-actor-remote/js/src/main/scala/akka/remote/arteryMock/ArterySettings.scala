/**
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.remote.artery

import com.typesafe.config.Config

/** INTERNAL API */
private[akka] final case class ArterySettings(config: Config) {

  val Enabled = false

}
