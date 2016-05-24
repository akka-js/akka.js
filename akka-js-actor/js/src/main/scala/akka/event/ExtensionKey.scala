package akka.event

import scala.reflect.ClassTag

//Another dirty hack sorry...
abstract class ExtensionKey[T <: akka.actor.Extension](implicit m: ClassTag[T]) extends akka.actor.ExtensionKey[T] {
  var loggerId = 0

  override def apply(system: akka.actor.ActorSystem): T = (
    new Logging.LogExt(system.asInstanceOf[akka.actor.ExtendedActorSystem]) {
      override def id() = {
        loggerId += 1
        loggerId
      }
    }
  ).asInstanceOf[T]
}