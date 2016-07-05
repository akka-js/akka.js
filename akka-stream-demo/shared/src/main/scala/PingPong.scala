package eu.unicredit
import akka.actor._
import com.typesafe.config.Config

object PingPong{


  def ppActor(matcher: String, answer: String) = Props(
      new Actor {
        def receive = {
          case matcher =>
            sender ! answer
            println(s"received $matcher sending answer $answer")
        }
      }
    )

  def start(implicit system: ActorSystem) = {

    val ponger = system.actorOf(ppActor("ping", "pong"))
    val pinger = system.actorOf(ppActor("pong", "ping"))

    import system.dispatcher

    import scala.concurrent.duration._
    system.scheduler.scheduleOnce(1 second)(
      pinger.!("pong")(ponger)
    )

    system.scheduler.scheduleOnce(2 seconds){
      pinger ! PoisonPill
      ponger ! PoisonPill
    }
  }

}
