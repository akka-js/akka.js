package akka.dispatch

import scala.collection.mutable.Queue

class AbstractNodeQueue[A >: Null] extends Queue[A] {
  def add(value: A) = super.enqueue(value)

  def poll(): A = if(isEmpty) null else super.dequeue()

  def count() = size

  def peek() = super.headOption.orNull
}
