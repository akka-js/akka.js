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

  final val ErrorLevel = akka.event.Logging.LogLevel(1)
  final val WarningLevel = akka.event.Logging.LogLevel(2)
  final val InfoLevel = akka.event.Logging.LogLevel(3)
  final val DebugLevel = akka.event.Logging.LogLevel(4)

  type MDC = Map[String, Any]

  val emptyMDC: MDC = Map()
}
