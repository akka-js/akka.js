package ch.epfl.lamp.scalajs.actors

final case class Envelope private (val message: Any, val sender: ActorRef)

object Envelope {
  def apply(message: Any, sender: ActorRef, system: ActorSystem): Envelope = {
    if (message == null) throw new InvalidMessageException("Message is null")
    new Envelope(message, if (sender ne Actor.noSender) sender else system.deadLetters)
  }
}
