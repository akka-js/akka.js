package org.reactivestreams

/** A [[Subscription]] represents a one-to-one lifecycle of a [[Subscriber]]
  * subscribing to a [[Publisher]].
  *
  * It can be used only once by a single [[Subscriber]]. It is used
  * for both signaling demand for data and for canceling demand (and allow
  * resource cleanup).
  */
trait Subscription {
  /** No events will be sent by a [[Publisher]] until demand is signaled via this method.
    *
    * It can be called however often and whenever needed—but the outstanding cumulative
    * demand must never exceed Long.MAX_VALUE. An outstanding cumulative demand
    * of Long.MAX_VALUE may be treated by the [[Publisher]] as "effectively unbounded".
    *
    * Whatever has been requested can be sent by the [[Publisher]] so
    * only signal demand for what can be safely handled.
    *
    * A [[Publisher]] can send less than is requested if the stream ends but
    * then must emit either [[Subscriber.onError]] or [[Subscriber.onComplete]].
    *
    * @param n the strictly positive number of elements to requests to
    *          the upstream [[Publisher]].
    */
  def request(n: Long): Unit

  /** Request the [[Publisher]] to stop sending data and clean up resources.
    *
    * Data may still be sent to meet previously signalled demand after
    * calling cancel as this request is asynchronous.
    */
  def cancel(): Unit
}
