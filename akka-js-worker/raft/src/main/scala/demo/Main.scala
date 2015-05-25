package demo

import akka.actor._
//import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.Props
//import scala.concurrent.Await
import scala.concurrent.duration._
import java.util.Calendar
import scala.language.postfixOps
import raft._
import scala.util.Success
import scala.util.Failure
import scala.scalajs.js
import com.typesafe.config.ConfigFactory
import akka.worker._

package object fn extends js.GlobalScope {
  def transition(from: Int, to: Int) = js.native
  def transitionHeartBeat(from: Int) = js.native
  def setState(id: Int, role: String) = js.native
  def setText(txt: String) = js.native
}

class Sequencer(ui: ActorRef) extends Actor with RaftClient /*with ActorLogging*/ {
  import context._

  def schedule = system.scheduler.scheduleOnce(5000 millis, self, "sequence")

  override def preStart() = schedule
  override def postRestart(reason: Throwable) = {}

  def receive = {
    case "sequence" =>
      decide("get") onComplete {
        case Success(x) => ui ! x
        case Failure(t) => println(s"Error ${t.getMessage()}")
      }

      schedule
  }
}

class Manager extends Actor {
  import demo.fn
  def receive = {
    case UIState(id, role) => fn.setState(id, role.getClass().getSimpleName.toLowerCase())
    case UIHeartbeat(from) => fn.transitionHeartBeat(from)
    case x => fn.setText(x.toString())
  }
}

@scala.scalajs.js.annotation.JSExportAll
object Main extends scala.scalajs.js.JSApp {
  def main(): Unit = {
    implicit val system = ActorSystem("UI", ConfigFactory.parseString("{\"akka\":{\"actor\":{\"provider\":\"akka.worker.WorkerActorRefProvider\"}}}"))
    val ui = system.actorOf(Props[Manager], "manager")
    AkkaWorkerMaster("worker.js")
  }
  
  def worker(): Unit = {
    val system = ActorSystem("other", ConfigFactory.parseString("{\"akka\":{\"actor\":{\"provider\":\"akka.worker.WorkerActorRefProvider\"}}}"))
    implicit val system2 = ActorSystem("main")
    val ui = system.actorFor("akka.cm://UI@127.0.0.1:0/user/manager")
    val members = Raft(3, ui)(system2)
    val client = system2.actorOf(Props(new Sequencer(ui)), "client")

  }
}