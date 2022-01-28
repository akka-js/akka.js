/**
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.dispatch

import java.util.ArrayDeque

class AbstractBoundedNodeQueue[T](capacity: Int) extends ArrayDeque[T](capacity) {
  override def offerFirst(e: T): Boolean = {
    if (size == capacity) false
    else super.offerFirst(e)
  }

  override def offerLast(e: T): Boolean = {
    if (size == capacity) false
    else super.offerLast(e)
  }

  override def add(e: T): Boolean = offerLast(e)
}
