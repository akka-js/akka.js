package akka.actor

import akka.testkit.{Await, ThreadUtil}
import scala.concurrent.duration._

object Thread {

  def sleep(duration :Long)(implicit system: ActorSystem): Unit  = ThreadUtil.sleep(duration)(system)

}

