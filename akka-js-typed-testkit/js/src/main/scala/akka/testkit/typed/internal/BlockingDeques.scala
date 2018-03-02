package akka.testkit.typed.internal

import java.util.concurrent.BlockingDeque
import java.util.ArrayDeque
import java.util.concurrent.TimeUnit

class LinkedBlockingDeque[T] extends ArrayDeque[T] with BlockingDeque[T] {
  def offer(elem: T, t: Long, u: TimeUnit): Boolean =
    offer(elem)

  def offerFirst(elem: T, t: Long, u: TimeUnit): Boolean =
    offerFirst(elem)

  def offerLast(elem: T, t: Long, u: TimeUnit): Boolean =
    offerLast(elem)

  def poll(t: Long, u: TimeUnit): T =
    remove()

  // scala parenthesis management is ways too smart
  override def pollFirst: T =
    removeFirst()

  def pollFirst(t: Long, u: TimeUnit): T =
    removeFirst()

  def pollLast(t: Long, u: TimeUnit): T =
    removeLast()

  def put(elem: T): Unit =
    offer(elem)

  def putFirst(elem: T): Unit =
    offerFirst(elem)

  def putLast(elem: T): Unit =
    offerLast(elem)

  def take(): T =
    poll()

  def takeFirst(): T =
    pollFirst()

  def takeLast(): T =
    pollLast()

  def drainTo(x$1: java.util.Collection[_ >: T],x$2: Int): Int = ???
  def drainTo(x$1: java.util.Collection[_ >: T]): Int = ???
  def remainingCapacity(): Int = ???
}
