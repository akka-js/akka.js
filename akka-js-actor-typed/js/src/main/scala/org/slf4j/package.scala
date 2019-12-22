package org.slf4j

import java.util.{logging => jl}

class Logger(name: String = "default") {
  private val inner = wvlet.log.Logger(name)

  def error(message: Any, more: Any*): Unit = inner.error(message + more.mkString(" "))
  def error(message: Any, fmt: String, more: Any*): Unit = inner.error(message + more.mkString(" "))
  
  def warn(message: Any, more: Any*): Unit = inner.warn(message + more.mkString(" "))
  def warn(message: Any, fmt: String, more: Any*): Unit = inner.warn(message + more.mkString(" "))
  
  def info(message: Any, more: Any*): Unit = inner.info(message + more.mkString(" "))
  def info(message: Any, fmt: String, more: Any*): Unit = inner.info(message + more.mkString(" "))
  
  def debug(message: Any, more: Any*): Unit = inner.debug(message + more.mkString(" "))
  def debug(message: Any, fmt: String, more: Any*): Unit = inner.debug(message + more.mkString(" "))
  
  def trace(message: Any, more: Any*): Unit = inner.trace(message + more.mkString(" "))
  def trace(message: Any, fmt: String, more: Any*): Unit = inner.trace(message + more.mkString(" "))

  def isDebugEnabled() = false

}

object Logger {
  val ROOT_LOGGER_NAME = "root"
}

object LoggerFactory {

  def getLogger(name: String) = new Logger(name)
  def getLogger(clz: Class[_]) = new Logger(clz.getSimpleName)

}

class Marker {}

object MDC extends scala.collection.mutable.HashMap[String, String]
