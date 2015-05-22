package akkaWorkerMainJS

import scala.scalajs.js
import akka.actor._
import akka.worker.AkkaWorkerMaster
import com.typesafe.config.ConfigFactory

class A extends Actor {
  import context.system
  def receive = {
    case "back" =>
      println("HEARD BACK")
    case m => 
      println(s"GOTCHA ${m}")
      val remoteActor2 = system.actorFor("akka.cm://worker@127.0.0.1:2/user/kartoffeln2")
      remoteActor2 ! "LOL YO"
  }
}

class B extends Actor {
  def receive = {
    case m => 
      println(s"GOTCHA2 ${m}")
      sender() ! "back"
  }
}

@scala.scalajs.js.annotation.JSExportAll
object WebApp extends js.JSApp {
  def main(): Unit = {
    val s = ActorSystem("main", ConfigFactory.parseString("{\"akka\":{\"actor\":{\"provider\":\"akka.worker.WorkerActorRefProvider\"}}}"))
    AkkaWorkerMaster("worker.js")
    AkkaWorkerMaster("worker2.js")
    
    val remoteActor = s.actorFor("akka.cm://worker@127.0.0.1:1/user/kartoffeln")
    
    remoteActor ! 2  

  }
  
  def worker(): Unit = {
    val s = ActorSystem("worker", ConfigFactory.parseString("{\"akka\":{\"actor\":{\"provider\":\"akka.worker.WorkerActorRefProvider\"}}}"))
    
    s.actorOf(Props[A], "kartoffeln")
  }
  
  def worker2(): Unit = {
    val s = ActorSystem("worker", ConfigFactory.parseString("{\"akka\":{\"actor\":{\"provider\":\"akka.worker.WorkerActorRefProvider\"}}}"))
    
    s.actorOf(Props[B], "kartoffeln2")
  }
}
