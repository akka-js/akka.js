package akka.stream.actor

import akka.actor.{ActorSystem, ExtendedActorSystem}

//I know this is an hack...
trait ExtensionId[T <: akka.actor.Extension] extends akka.actor.ExtensionId[T] {
  self: akka.actor.ExtensionIdProvider =>

  override def apply(system: ActorSystem): T = {
    createExtension(system.asInstanceOf[ExtendedActorSystem])
  }
}
