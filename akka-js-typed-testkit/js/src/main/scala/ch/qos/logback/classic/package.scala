package ch.qos.logback.classic

import java.util.{logging => jul}

class Logger(name: String = "default") {
  private val inner = wvlet.log.Logger(name)
  def getName() = name
}

object Level {
  val TRACE_INT = org.slf4j.event.Level.TRACE.order
  val DEBUG_INT = org.slf4j.event.Level.DEBUG.order
  val INFO_INT  = org.slf4j.event.Level.INFO.order
  val WARN_INT  = org.slf4j.event.Level.WARN.order
  val ERROR_INT = org.slf4j.event.Level.ERROR.order
}

class Level(order: Int, jlLevel: jul.Level, name: String) extends org.slf4j.event.Level(order, jlLevel, name) {
  def levelInt = this.order
  def levelStr= this.name
}
