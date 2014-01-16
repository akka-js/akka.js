package org.scalajs.actors.event

import language.existentials

object Logging {

  /**
   * Marker trait for annotating LogLevel, which must be Int after erasure.
   */
  case class LogLevel(asInt: Int) extends AnyVal {
    @inline final def >=(other: LogLevel): Boolean = asInt >= other.asInt
    @inline final def <=(other: LogLevel): Boolean = asInt <= other.asInt
    @inline final def >(other: LogLevel): Boolean = asInt > other.asInt
    @inline final def <(other: LogLevel): Boolean = asInt < other.asInt
  }

  /**
   * Log level in numeric form, used when deciding whether a certain log
   * statement should generate a log event. Predefined levels are ErrorLevel (1)
   * to DebugLevel (4). In case you want to add more levels, loggers need to
   * be subscribed to their event bus channels manually.
   */
  final val ErrorLevel = LogLevel(1)
  final val WarningLevel = LogLevel(2)
  final val InfoLevel = LogLevel(3)
  final val DebugLevel = LogLevel(4)

  /**
   * Base type of LogEvents
   */
  sealed trait LogEvent {
    /**
     * The thread that created this log event
     */
    @transient
    val thread: Thread = Thread.currentThread

    /**
     * When this LogEvent was created according to System.currentTimeMillis
     */
    val timestamp: Long = System.currentTimeMillis

    /**
     * The LogLevel of this LogEvent
     */
    def level: LogLevel

    /**
     * The source of this event
     */
    def logSource: String

    /**
     * The class of the source of this event
     */
    def logClass: Class[_]

    /**
     * The message, may be any object or null.
     */
    def message: Any
  }

  /**
   * For ERROR Logging
   */
  case class Error(cause: Throwable, logSource: String, logClass: Class[_], message: Any = "") extends LogEvent {
    def this(logSource: String, logClass: Class[_], message: Any) = this(Error.NoCause, logSource, logClass, message)

    override def level = ErrorLevel
  }

  object Error {
    def apply(logSource: String, logClass: Class[_], message: Any) =
      new Error(NoCause, logSource, logClass, message)

    /** Null Object used for errors without cause Throwable */
    object NoCause extends Throwable
  }
  def noCause = Error.NoCause

  /**
   * For WARNING Logging
   */
  case class Warning(logSource: String, logClass: Class[_], message: Any = "") extends LogEvent {
    override def level = WarningLevel
  }

  /**
   * For INFO Logging
   */
  case class Info(logSource: String, logClass: Class[_], message: Any = "") extends LogEvent {
    override def level = InfoLevel
  }

  /**
   * For DEBUG Logging
   */
  case class Debug(logSource: String, logClass: Class[_], message: Any = "") extends LogEvent {
    override def level = DebugLevel
  }

}
