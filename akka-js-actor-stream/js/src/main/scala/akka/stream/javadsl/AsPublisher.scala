/**
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.javadsl

sealed trait AsPublisher
object AsPublisher {
  case object WITH_FANOUT extends AsPublisher
  case object WITHOUT_FANOUT extends AsPublisher
}

