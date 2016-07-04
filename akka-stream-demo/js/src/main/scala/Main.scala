package eu.unicredit

import akka.actor.ActorSystem

import scala.scalajs.js

object Main extends js.JSApp {

  def main() = {
    println("JS env")
    //PingPong1.start(AkkaConfig.config)
    Run.run(AkkaConfig.config)
    //ChatUI.start(AkkaConfig.config)
  }

}