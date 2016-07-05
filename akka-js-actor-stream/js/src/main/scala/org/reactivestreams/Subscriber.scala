package org.reactivestreams

/** Will receive call to [[Subscriber!.onSubscribe onSubscribe]]
  * once after passing an instance of [[Subscriber]] to [[Publisher.subscribe]].
  *
  * No further notifications will be received until [[Subscription.request]] is called.
  *
  * After signaling demand:
  *
  *  - One or more invocations of [[Subscriber!.onNext onNext]] up to the
  *    maximum number defined by [[Subscription.request]]
  *  - Single invocations of [[Subscriber!.onError onError]] or
  *    [[Subscriber!.onComplete onComplete]] which signals a terminal state after
  *    which no furter events will be sent.
  *
  * Demand can be signaled via [[Subscription.request]] whenever the
  * [[Subscriber]] instance is capable of handling more.
  *
  * @tparam T the type of element signaled.
  */
trait Subscriber[T] {
  /** Invoked after calling [[Publisher.subscribe]].
    *
    * No data will start flowing until [[Subscription.request]] is invoked.
    *
    * It is the responsibility of this [[Subscriber]] instance
    * to call [[Subscription.request]] whenever more data is wanted.
    *
    * The [[Publisher]] will send notifications only
    * in response to [[Subscription.request]].
    *
    * @param s [[Subscription]] that allows requesting data via [[Subscription.request]]
    */
  def onSubscribe(s: Subscription): Unit

  /** Data notification sent by the [[Publisher]] in response to
    * requests to [[Subscription.request]].
    *
    * @param t the element signaled
    */
  def onNext(t: T): Unit

  /** Failed terminal state.
    *
    * No further events will be sent even
    * if [[Subscription.request]] is invoked again.
    *
    * @param t the throwable signaled
    */
  def onError(t: Throwable): Unit

  /** Successful terminal state.
    *
    * No further events will be sent even
    * if [[Subscription.request]] is invoked again.
    */
  def onComplete(): Unit
}
