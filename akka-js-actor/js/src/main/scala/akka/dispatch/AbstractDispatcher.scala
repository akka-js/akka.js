/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.dispatch

import akka.event.Logging.{ Debug, Error, LogEventException }
import akka.actor._
import akka.dispatch.sysmsg._
import akka.event.{ BusLogging, EventStream }
 /**import com.typesafe.config.{ ConfigFactory, Config } */
import akka.util.{ /** @note IMPLEMENT IN SCALA.JS Unsafe,*/ Index }
import akka.event.EventStream
import com.typesafe.config.Config
import scala.annotation.tailrec
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js.timers._
import scala.util.control.NonFatal
import scala.util.Try
import java.{ util ⇒ ju }
import java.util.concurrent.Executor

final case class Envelope private (val message: Any, val sender: ActorRef)

object Envelope {
  def apply(message: Any, sender: ActorRef, system: ActorSystem): Envelope = {
    if (message == null) throw new InvalidMessageException("Message is null")
    new Envelope(message, if (sender ne Actor.noSender) sender else system.deadLetters)
  }
}

final case class TaskInvocation(eventStream: EventStream, runnable: Runnable, cleanup: () ⇒ Unit) extends Runnable /** @note IMPLEMENT IN SCALA.JS extends Batchable */ {
	final def isBatchable: Boolean = runnable match {
    case _                                      ⇒ false  
  }

  def run(): Unit =
    try runnable.run() catch {
      case NonFatal(e) ⇒ eventStream.publish(Error(e, "TaskInvocation", this.getClass, e.getMessage))
    } finally cleanup()
}



/**
 * INTERNAL API
 */
 private[akka] trait LoadMetrics { self: Executor ⇒
   def atFullThrottle(): Boolean
 }

/**
 * INTERNAL API
 */

private[akka] object MessageDispatcher {
	val UNSCHEDULED = 0 //WARNING DO NOT CHANGE THE VALUE OF THIS: It relies on the faster init of 0 in AbstractMessageDispatcher
  val SCHEDULED = 1
	val RESCHEDULED = 2

	// dispatcher debugging helper using println (see below)
	// since this is a compile-time constant, scalac will elide code behind if (MessageDispatcher.debug) (RK checked with 2.9.1)
  final val debug = false // Deliberately without type ascription to make it a compile-time constant
	lazy val actors = new Index[MessageDispatcher, ActorRef](16, new ju.Comparator[ActorRef] {
    override def compare(a: ActorRef, b: ActorRef): Int = a.compareTo(b)
  })
  def printActors(): Unit =
    if (debug) {
      for {
        d ← actors.keys
        a ← { println(d + " inhabitants: " + d.inhabitants); actors.valueIterator(d) }
      } {
        val status = if (a.isTerminated) " (terminated)" else " (alive)"
        val messages = a match {
          case r: ActorRefWithCell ⇒ " " + r.underlying.numberOfMessages + " messages"
          case _                   ⇒ " " + a.getClass
        }
        val parent = a match {
          case i: InternalActorRef ⇒ ", parent: " + i.getParent
          case _                   ⇒ ""
        }
        println(" -> " + a + status + messages + parent)
      }
    }
}

abstract class MessageDispatcher(val configurator: MessageDispatcherConfigurator) extends /** @note IMPLEMENT IN SCALA.JS  AbstractMessageDispatcher with BatchingExecutor with */ ExecutionContextExecutor {

	import MessageDispatcher._
	/** @note IMPLEMENT IN SCALA.JS
  import AbstractMessageDispatcher.{ inhabitantsOffset, shutdownScheduleOffset }
  */
	import configurator.prerequisites

	val mailboxes = prerequisites.mailboxes
	val eventStream = prerequisites.eventStream

	@volatile private[this] var _inhabitantsDoNotCallMeDirectly: Long = _ // DO NOT TOUCH!
	@volatile private[this] var _shutdownScheduleDoNotCallMeDirectly: Int = _ // DO NOT TOUCH!

	/** @note IMPLEMENT IN SCALA.JS @tailrec */ private final def addInhabitants(add: Long): Long = {
		val c = inhabitants
		val r = c + add

    if (r < 0) {
		  // We haven't succeeded in decreasing the inhabitants yet but the simple fact that we're trying to
		  // go below zero means that there is an imbalance and we might as well throw the exception
		  val e = new IllegalStateException("ACTOR SYSTEM CORRUPTED!!! A dispatcher can't have less than 0 inhabitants!")
		  reportFailure(e)
		  throw e
		}

    /** @note IMPLEMENT IN SCALA.JS
	  if (Unsafe.instance.compareAndSwapLong(this, inhabitantsOffset, c, r)) r else addInhabitants(add)
    */
    _inhabitantsDoNotCallMeDirectly = r
    r
	}

  /** @note IMPLEMENT IN SCALA.JS
	final def inhabitants: Long = Unsafe.instance.getLongVolatile(this, inhabitantsOffset)
  */
  final def inhabitants: Long = _inhabitantsDoNotCallMeDirectly

  /** @note IMPLEMENT IN SCALA.JS
	private final def shutdownSchedule: Int = Unsafe.instance.getIntVolatile(this, shutdownScheduleOffset)
	private final def updateShutdownSchedule(expect: Int, update: Int): Boolean = Unsafe.instance.compareAndSwapInt(this, shutdownScheduleOffset, expect, update)
  */
  private final def shutdownSchedule: Int = _shutdownScheduleDoNotCallMeDirectly
  private final def updateShutdownSchedule(expect: Int, update: Int): Boolean = {
    _shutdownScheduleDoNotCallMeDirectly = update
    true
  }

	/**
	 *  Creates and returns a mailbox for the given actor.
	 */
	protected[akka] def createMailbox(actor: Cell, mailboxType: MailboxType): Mailbox

	/**
	 * Identifier of this dispatcher, corresponds to the full key
	 * of the dispatcher configuration.
	 */
	def id: String

	/**
	 * Attaches the specified actor instance to this dispatcher, which includes
	 * scheduling it to run for the first time (Create() is expected to have
	 * been enqueued by the ActorCell upon mailbox creation).
	 */
	final def attach(actor: ActorCell): Unit = {
		register(actor)
		registerForExecution(actor.mailbox, false, true)
	}

	/**
	 * Detaches the specified actor instance from this dispatcher
	 */
	final def detach(actor: ActorCell): Unit = try unregister(actor) finally ifSensibleToDoSoThenScheduleShutdown()

  /** THIS COMES FROM BatchingExecutor */
  override def execute(runnable: Runnable): Unit = unbatchedExecute(runnable)
  /** END */

	final /** @note IMPLEMENT IN SCALA.JS override */ protected def unbatchedExecute(r: Runnable): Unit = {
	  val invocation = TaskInvocation(eventStream, r, taskCleanup)
		addInhabitants(+1)
		try {
		  executeTask(invocation)
		} catch {
		  case t: Throwable ⇒
		    addInhabitants(-1)
				throw t
		}
	}

	override def reportFailure(t: Throwable): Unit = t match {
	/** @note IMPLEMENT IN SCALA.JS case e: LogEventException ⇒ eventStream.publish(e.event) */
	case _                    ⇒ eventStream.publish(Error(t, getClass.getName, getClass, t.getMessage))
	}

	@tailrec
	private final def ifSensibleToDoSoThenScheduleShutdown(): Unit = {
		if (inhabitants <= 0) shutdownSchedule match {
		  case UNSCHEDULED ⇒
		    if (updateShutdownSchedule(UNSCHEDULED, SCHEDULED)) scheduleShutdownAction()
		    else ifSensibleToDoSoThenScheduleShutdown()
		  case SCHEDULED ⇒
		    if (updateShutdownSchedule(SCHEDULED, RESCHEDULED)) ()
		    else ifSensibleToDoSoThenScheduleShutdown()
		  case RESCHEDULED ⇒
		}
	}

	private def scheduleShutdownAction(): Unit = {
		// IllegalStateException is thrown if scheduler has been shutdown
		try prerequisites.scheduler.scheduleOnce(shutdownTimeout, shutdownAction)(new ExecutionContext {
			override def execute(runnable: Runnable): Unit = scalajs.js.timers.setTimeout(0) { runnable.run() } // @note IMPLEMENT IN SCALA.JS runnable.run()
			override def reportFailure(t: Throwable): Unit = MessageDispatcher.this.reportFailure(t)
		}) catch {
		  case _: IllegalStateException ⇒ shutdown()
		}
	}

	private final val taskCleanup: () ⇒ Unit = () ⇒ if (addInhabitants(-1) == 0) ifSensibleToDoSoThenScheduleShutdown()

	/**
	 * If you override it, you must call it. But only ever once. See "attach" for only invocation.
	 *
	 * INTERNAL API
	 */
	protected[akka] def register(actor: ActorCell) {
	  /**@note IMPLEMENT IN SCALA.JS if (debug) actors.put(this, actor.self) */
	  addInhabitants(+1)
  }

	/**
	 * If you override it, you must call it. But only ever once. See "detach" for the only invocation
	 *
	 * INTERNAL API
	 */
	protected[akka] def unregister(actor: ActorCell) {
		/**@note IMPLEMENT IN SCALA.JS if (debug) actors.remove(this, actor.self) */
		addInhabitants(-1)
		val mailBox = actor.swapMailbox(mailboxes.deadLetterMailbox)
		mailBox.becomeClosed()
		mailBox.cleanUp()
	}

	private val shutdownAction = new Runnable {
		@tailrec
		final def run() {
			shutdownSchedule match {
			case SCHEDULED ⇒
			  try {
				  if (inhabitants == 0) shutdown() //Warning, racy
			  } finally {
				  while (!updateShutdownSchedule(shutdownSchedule, UNSCHEDULED)) {}
			  }
			case RESCHEDULED ⇒
			  if (updateShutdownSchedule(RESCHEDULED, SCHEDULED)) scheduleShutdownAction()
			  else run()
			case UNSCHEDULED ⇒
      }
		}
	}

	/**
	 * When the dispatcher no longer has any actors registered, how long will it wait until it shuts itself down,
	 * defaulting to your akka configs "akka.actor.default-dispatcher.shutdown-timeout" or default specified in
	 * reference.conf
	 *
	 * INTERNAL API
	 */
	protected[akka] def shutdownTimeout: FiniteDuration

	/**
	 * After the call to this method, the dispatcher mustn't begin any new message processing for the specified reference
	 */
	protected[akka] def suspend(actor: ActorCell): Unit = {
		val mbox = actor.mailbox
		if ((mbox.actor eq actor) && (mbox.dispatcher eq this))
		mbox.suspend()
	}

	/*
	 * After the call to this method, the dispatcher must begin any new message processing for the specified reference
	 */
	protected[akka] def resume(actor: ActorCell): Unit = {
		val mbox = actor.mailbox
		if ((mbox.actor eq actor) && (mbox.dispatcher eq this) && mbox.resume())
		registerForExecution(mbox, false, false)
	}

	/**
	 * Will be called when the dispatcher is to queue an invocation for execution
	 *
	 * INTERNAL API
	 */
	protected[akka] def systemDispatch(receiver: ActorCell, invocation: SystemMessage)

	/**
	 * Will be called when the dispatcher is to queue an invocation for execution
	 *
	 * INTERNAL API
	 */
	protected[akka] def dispatch(receiver: ActorCell, invocation: Envelope)

	/**
	 * Suggest to register the provided mailbox for execution
	 *
	 * INTERNAL API
	 */
	protected[akka] def registerForExecution(mbox: Mailbox, hasMessageHint: Boolean, hasSystemMessageHint: Boolean): Boolean

	// TODO check whether this should not actually be a property of the mailbox
	/**
	 * INTERNAL API
	 */
	protected[akka] def throughput: Int

	/**
	 * INTERNAL API
	 */
	protected[akka] def throughputDeadlineTime: Duration

	/**
	 * INTERNAL API
	 */
	@inline protected[akka] final val isThroughputDeadlineTimeDefined = throughputDeadlineTime.toMillis > 0

	/**
	 * INTERNAL API
	 */
  protected[akka] def executeTask(invocation: TaskInvocation)

	/**
	 * Called one time every time an actor is detached from this dispatcher and this dispatcher has no actors left attached
	 * Must be idempotent
	 *
	 * INTERNAL API
	 */
	protected[akka] def shutdown(): Unit
}


/**
 * An ExecutorServiceConfigurator is a class that given some prerequisites and a configuration can create instances of ExecutorService
 */
abstract class ExecutorServiceConfigurator(config: Config, prerequisites: DispatcherPrerequisites) extends ExecutorServiceFactoryProvider


/**
 * Base class to be used for hooking in new dispatchers into Dispatchers.
 */
abstract class MessageDispatcherConfigurator(_config: Config, val prerequisites: DispatcherPrerequisites) {

  /** @note IMPLEMENT IN SCALA.JS
   *  val config: Config = new CachingConfig(_config)
   */
  val config: Config = _config

  /**
   * Returns an instance of MessageDispatcher given the configuration.
   * Depending on the needs the implementation may return a new instance for
   * each invocation or return the same instance every time.
   */
  def dispatcher(): MessageDispatcher

  def configureExecutor(): ExecutorServiceConfigurator = {
    def configurator(executor: String): ExecutorServiceConfigurator = executor match {
      case "event-loop-executor" ⇒ new EventLoopExecutorConfigurator(new Config, prerequisites)
    }
    configurator("event-loop-executor")
  }
}
