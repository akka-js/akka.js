package akka.util

import akka.actor.ActorSystem
import akka.testkit.ThreadUtil

object Thread {

  def sleep(duration :Long)(implicit system: ActorSystem): Unit  = ThreadUtil.sleep(duration)(system)

}
