package akka.util

import scala.collection.mutable

import scala.scalajs.js

class JSQueue[A] {
  private[this] var queue: js.Array[A] = js.Array()
  private[this] var offset: Int = 0

  def size: Int = queue.length.toInt - offset

  def isEmpty: Boolean = queue.length.toInt == 0
  def nonEmpty: Boolean = queue.length.toInt != 0

  def enqueue(item: A): Unit = queue.push(item)

  def dequeue(): A = {
    val queueLength = queue.length.toInt
    if (queueLength == 0)
      throw new NoSuchElementException("queue empty")

    val item = queue(offset)
    offset += 1

    // shrink the underlying queue if necessary
    if (offset*2 >= queueLength) {
      queue = queue.slice(offset, queueLength)
      offset = 0
    }

    item
  }
}
