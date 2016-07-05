package eu.unicredit

import akka.actor.ActorSystem
import com.typesafe.config.Config

object Run {

  def run(akkaConfig: Config) = {
    implicit val system = ActorSystem("streams", akkaConfig)
    PingPong.start
    Streams.simpleFlow
    Streams.complexFlow(false)
  }
}