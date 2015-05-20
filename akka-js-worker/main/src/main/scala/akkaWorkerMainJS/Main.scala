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

@scala.scalajs.js.annotation.JSExportAll
object WebApp extends js.JSApp {
  def main(): Unit = {
    val s = ActorSystem("main", ConfigFactory.parseString("{\"akka\":{\"actor\":{\"provider\":\"akka.worker.WorkerActorRefProvider\"}}}"))
    AkkaWorker("worker.js")
    
    //s.actorOf(Props[A], "kartoffeln") ! "HELLO"
    val remoteActor = s.actorFor("akka.cm://worker@127.0.0.1:1/user/kartoffeln")
    
    remoteActor ! "HEY YO"
    

  }
  
  def worker(): Unit = {
    //val s = ActorSystem("lol", ConfigFactory.parseString("{\"akka\":{\"actor\":{\"provider\":\"akka.worker.WorkerActorRefProvider\"}}}"))
    //val remoteActor = s.actorFor("akka.cm://lol@127.0.0.1:1/user/kartoffeln")
    
    //remoteActor ! "LOLOLLOL"
    val s = ActorSystem("worker", ConfigFactory.parseString("{\"akka\":{\"actor\":{\"provider\":\"akka.worker.WorkerActorRefProvider\"}}}"))
    
    s.actorOf(Props[A], "kartoffeln")
    //val remoteActor = s.actorFor("akka.cm://lol@127.0.0.1:1/user/kartoffeln")
  }
}