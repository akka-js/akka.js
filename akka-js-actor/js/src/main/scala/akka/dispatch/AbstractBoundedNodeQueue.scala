/**
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.dispatch

import java.util.ArrayDeque

class AbstractBoundedNodeQueue[T](capacity: Int)
  extends ArrayDeque[T](capacity) {}
