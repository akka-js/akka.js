/**
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.dispatch

import scalajs.js

class AbstractBoundedNodeQueue[T](capacity: Int)
  extends java.util.ArrayDeque[T](capacity) {}
