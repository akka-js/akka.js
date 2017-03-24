/**
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.event.jul

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.dispatch.RequiresMessageQueue
import akka.event.DummyClassForStringSources
import akka.event.EventStream
import akka.event.LoggerMessageQueueSemantics
import akka.event.JSDefaultLogger
import akka.event.JSDefaultLoggingFilter
import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

/**
 * `java.util.logging` logger.
 */
@EnableReflectiveInstantiation
class JavaLogger extends JSDefaultLogger()

@EnableReflectiveInstantiation
class JavaLoggingFilter(settings: ActorSystem.Settings, eventStream: EventStream)
  extends JSDefaultLoggingFilter(settings, eventStream)
