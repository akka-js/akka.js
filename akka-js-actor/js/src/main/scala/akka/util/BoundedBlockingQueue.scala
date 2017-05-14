/**
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.util

import java.util.{ Queue, ArrayDeque }

/**
 * BoundedBlockingQueue wraps any Queue and turns the result into a BlockingQueue with a limited capacity.
 * @param maxCapacity - the maximum capacity of this Queue, needs to be &gt; 0
 * @param backing - the backing Queue
 */
 class BoundedBlockingQueue[E <: AnyRef](
   val maxCapacity: Int, private val backing: Queue[E]) extends ArrayDeque[E](maxCapacity)
