package akka.testkit.typed

import scala.concurrent.duration._
import akka.actor.typed.ActorSystem

object ThreadUtil {

  def sleep(duration :Long)(implicit system: ActorSystem[_]): Unit  = {
    import system.executionContext
    val p = scala.concurrent.Promise[Unit]
    system.scheduler.scheduleOnce(duration millis, new Runnable {
      def run() = p.success(())
    })
    akka.testkit.Await.result(p.future, (duration + 30) millis)
  }

}

