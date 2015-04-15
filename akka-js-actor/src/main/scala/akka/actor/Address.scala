package akka.actor

final case class Address private[akka] (
    system: String, worker: Option[String]) {

  /**
   * Returns true if this Address is only defined locally. It is not safe to
   * send locally scoped addresses to other workers.
   * See also [[org.scalajs.actors.Address#hasGlobalScope]].
   */
  def hasLocalScope: Boolean = worker.isEmpty

  /**
   * Returns true if this Address is usable globally. Unlike locally defined
   * addresses ([[org.scalajs.actors.Address#hasLocalScope]]), addresses of
   * global scope are safe to send to other workers, as they globally and
   * uniquely identify an addressable entity.
   */
  def hasGlobalScope: Boolean = worker.isDefined

}

object Address {
  def apply(system: String): Address =
    new Address(system, None)
  def apply(system: String, worker: String): Address =
    new Address(system, Some(worker))
}
