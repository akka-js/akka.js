/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.event

import Logging._
import akka.actor._
import akka.dispatch.RequiresMessageQueue
import akka.actor.ActorSystem.Settings
import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

@EnableReflectiveInstantiation
class JSDefaultLogger() extends Actor with StdOutLogger with RequiresMessageQueue[LoggerMessageQueueSemantics] {
  override def receive: Receive = {
    case InitializeLogger(_) ⇒ sender() ! LoggerInitialized
    case event: LogEvent     ⇒ print(event)
  }
}

@EnableReflectiveInstantiation
class JSDefaultLoggingFilter(settings: Settings, eventStream: EventStream) extends LoggingFilter {

  def logLevel(): Logging.LogLevel = eventStream.logLevel

  import Logging._
  def isErrorEnabled(logClass: Class[_], logSource: String) = logLevel() >= ErrorLevel
  def isWarningEnabled(logClass: Class[_], logSource: String) = logLevel() >= WarningLevel
  def isInfoEnabled(logClass: Class[_], logSource: String) = logLevel() >= InfoLevel
  def isDebugEnabled(logClass: Class[_], logSource: String) = logLevel() >= DebugLevel
}
