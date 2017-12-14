package akka.stream.scaladsl

import akka.testkit.ThreadUtil
import akka.actor.ActorSystem
import java.{lang => jl}

object Thread {

  def sleep(duration :Long)(implicit system: ActorSystem): Unit  = ThreadUtil.sleep(duration)(system)

  def currentThread(): jl.Thread = jl.Thread.currentThread()

}
