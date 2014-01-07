package org.scalajs.actors

abstract class ActorRef {

  /**
   * Sends a one-way asynchronous message. E.g. fire-and-forget semantics.
   * <p/>
   *
   * If invoked from within an actor then the actor reference is implicitly passed on as the implicit 'sender' argument.
   * <p/>
   *
   * This actor 'sender' reference is then available in the receiving actor in the 'sender' member variable,
   * if invoked from within an Actor. If not then no sender is available.
   * <pre>
   *   actor ! message
   * </pre>
   * <p/>
   */
  def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit

  /**
   * Forwards the message and passes the original sender actor as the sender.
   *
   * Works with '!' and '?'/'ask'.
   */
  def forward(message: Any)(implicit context: ActorContext): Unit =
    this.!(message)(context.sender)
}
