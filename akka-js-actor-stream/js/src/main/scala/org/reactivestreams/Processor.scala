package org.reactivestreams

/** A Processor represents a processing stage—which is both a [[Subscriber]]
  * and a [[Publisher]] and obeys the contracts of both.
  *
  * @tparam T the type of element signaled to the [[Subscriber]]
  * @tparam R the type of element signaled by the [[Publisher]]
  */
trait Processor[T,R] extends Subscriber[T] with Publisher[R]
