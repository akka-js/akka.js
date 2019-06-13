/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.pattern

import akka.actor.{ ActorSelection, Scheduler }
import scala.concurrent.ExecutionContext
import java.util.concurrent.Callable
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit.MILLISECONDS

object Patterns {
  import akka.actor.{ ActorRef, ActorSystem }
  import akka.pattern.{ ask ⇒ scalaAsk, pipe ⇒ scalaPipe, gracefulStop ⇒ scalaGracefulStop, after ⇒ scalaAfter, retry => scalaRetry }
  import akka.util.Timeout
  import scala.concurrent.Future
  import scala.concurrent.duration.Duration

  /**
   * <i>Java API for `akka.pattern.ask`:</i>
   * Sends a message asynchronously and returns a [[scala.concurrent.Future]]
   * holding the eventual reply message; this means that the target actor
   * needs to send the result to the `sender` reference provided. The Future
   * will be completed with an [[akka.pattern.AskTimeoutException]] after the
   * given timeout has expired; this is independent from any timeout applied
   * while awaiting a result for this future (i.e. in
   * `Await.result(..., timeout)`).
   *
   * <b>Warning:</b>
   * When using future callbacks, inside actors you need to carefully avoid closing over
   * the containing actor’s object, i.e. do not call methods or access mutable state
   * on the enclosing actor from within the callback. This would break the actor
   * encapsulation and may introduce synchronization bugs and race conditions because
   * the callback will be scheduled concurrently to the enclosing actor. Unfortunately
   * there is not yet a way to detect these illegal accesses at compile time.
   *
   * <b>Recommended usage:</b>
   *
   * {{{
   *   final Future<Object> f = Patterns.ask(worker, request, timeout);
   *   f.onSuccess(new Procedure<Object>() {
   *     public void apply(Object o) {
   *       nextActor.tell(new EnrichedResult(request, o));
   *     }
   *   });
   * }}}
   */
  def ask(actor: ActorRef, message: Any, timeout: Timeout): Future[AnyRef] = scalaAsk(actor, message)(timeout).asInstanceOf[Future[AnyRef]]

  /**
   * <i>Java API for `akka.pattern.ask`:</i>
   * Sends a message asynchronously and returns a [[scala.concurrent.Future]]
   * holding the eventual reply message; this means that the target actor
   * needs to send the result to the `sender` reference provided. The Future
   * will be completed with an [[akka.pattern.AskTimeoutException]] after the
   * given timeout has expired; this is independent from any timeout applied
   * while awaiting a result for this future (i.e. in
   * `Await.result(..., timeout)`).
   *
   * <b>Warning:</b>
   * When using future callbacks, inside actors you need to carefully avoid closing over
   * the containing actor’s object, i.e. do not call methods or access mutable state
   * on the enclosing actor from within the callback. This would break the actor
   * encapsulation and may introduce synchronization bugs and race conditions because
   * the callback will be scheduled concurrently to the enclosing actor. Unfortunately
   * there is not yet a way to detect these illegal accesses at compile time.
   *
   * <b>Recommended usage:</b>
   *
   * {{{
   *   final Future<Object> f = Patterns.ask(worker, request, timeout);
   *   f.onSuccess(new Procedure<Object>() {
   *     public void apply(Object o) {
   *       nextActor.tell(new EnrichedResult(request, o));
   *     }
   *   });
   * }}}
   */
  def ask(actor: ActorRef, message: Any, timeoutMillis: Long): Future[AnyRef] =
    scalaAsk(actor, message)(new Timeout(timeoutMillis, MILLISECONDS)).asInstanceOf[Future[AnyRef]]

  /**
   * Returns a [[scala.concurrent.Future]] that will be completed with the success or failure of the provided Callable
   * after the specified duration.
   */
  def after[T](duration: FiniteDuration, scheduler: Scheduler, context: ExecutionContext, value: Callable[Future[T]]): Future[T] =
    scalaAfter(duration, scheduler)(value.call())(context)

  /**
   * Returns a [[scala.concurrent.Future]] that will be completed with the success or failure of the provided Callable
   * after the specified duration.
   */
  def after[T](duration: FiniteDuration, scheduler: Scheduler, context: ExecutionContext, value: Future[T]): Future[T] =
    scalaAfter(duration, scheduler)(value)(context)

  /**
   * Returns an internally retrying [[scala.concurrent.Future]]
   * The first attempt will be made immediately, and each subsequent attempt will be made after 'delay'.
   * A scheduler (eg context.system.scheduler) must be provided to delay each retry
   * If attempts are exhausted the returned future is simply the result of invoking attempt.
   * Note that the attempt function will be invoked on the given execution context for subsequent tries and
   * therefore must be thread safe (not touch unsafe mutable state).
   */
  def retry[T](
      attempt: Callable[Future[T]],
      attempts: Int,
      delay: FiniteDuration,
      scheduler: Scheduler,
      context: ExecutionContext): Future[T] =
    scalaRetry(() => attempt.call, attempts, delay)(context, scheduler)

}
