package akkajs

import com.typesafe.config

object Config {

  val default: config.Config =
    config.ConfigFactory.load()

}
