package akka.routing

import akka.testkit.ThreadUtil
import akka.actor.ActorSystem

object Thread {

  def sleep(duration :Long)(implicit system: ActorSystem): Unit  = ThreadUtil.sleep(duration)(system)

}

