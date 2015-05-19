package akkaWorkerMainJS

import scala.scalajs.js
import akka.actor._
import akka.worker.AkkaWorker
import com.typesafe.config.ConfigFactory

class A extends Actor {
  def receive = {
    case m => println(s"GOTCHA ${m}")
  }
}

object WebApp extends js.JSApp {
  def main(): Unit = {
    val s = ActorSystem("lol", ConfigFactory.parseString("{\"akka\":{\"actor\":{\"provider\":\"akka.worker.WorkerActorRefProvider\"}}}"))
    
    s.actorOf(Props[A], "kartoffeln") ! "HELLO"
  }
  
  def worker(): Unit = {
    val s = ActorSystem("lol", ConfigFactory.parseString("{\"akka\":{\"actor\":{\"provider\":\"akka.worker.WorkerActorRefProvider\"}}}"))
    val remoteActor = s.actorFor("akka.cm://lol@127.0.0.1:1/user/kartoffeln")
    
    remoteActor ! "LOLOLLOL"
  }
}