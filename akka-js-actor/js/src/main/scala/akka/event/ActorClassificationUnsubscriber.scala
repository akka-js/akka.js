/**
 * Copyright (C) 2009-2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.event

import akka.actor._
import akka.event.Logging.simpleName
import java.util.concurrent.atomic.AtomicInteger

/**
 * INTERNAL API
 *
 * Provides factory for [[akka.event.ActorClassificationUnsubscriber]] actors with **unique names**.
 */
private[akka] object ActorClassificationUnsubscriber {

  //private val unsubscribersCount = new AtomicInteger(0)

  final case class Register(actor: ActorRef, seq: Int)
  final case class Unregister(actor: ActorRef, seq: Int)

  def start(system: ActorSystem, bus: ManagedActorClassification, debug: Boolean = false) = new {
    def !(any: Any) = ()
  }

  private def props(eventBus: ManagedActorClassification, debug: Boolean) = null//Props(classOf[ActorClassificationUnsubscriber], eventBus, debug)

}
