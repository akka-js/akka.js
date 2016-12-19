package akka.testkit

import scala.concurrent.duration._
import akka.actor.ActorSystem

object ThreadUtil {

  def sleep(duration :Long)(implicit  system: ActorSystem): Unit  = {
    import system.dispatcher
    val p = scala.concurrent.Promise[Unit]
    system.scheduler.scheduleOnce(duration millis){
      p.success(())
    }
    Await.result(p.future, (duration + 30) millis)
  }

}

