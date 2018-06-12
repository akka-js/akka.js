package akka.testkit

import akka.actor.ActorSystem

//Yes this is a true hack to let TestEventListener Cross Compile and fix it externally
object Logging {

  def apply(system: ActorSystem, clazz: Class[_]) =
    akka.event.Logging(system, clazz)

  class DefaultLogger extends akka.event.Logging.DefaultLogger {

    override def preStart() =
      self ! akka.event.Logging.InitializeLogger(context.system.eventStream)
  }

  object Error {
    val NoCause = akka.event.Logging.Error.NoCause
  }

  type MDC = Map[String, Any]

  val emptyMDC: MDC = Map()
}
