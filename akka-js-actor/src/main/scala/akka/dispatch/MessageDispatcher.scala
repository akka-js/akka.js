package akka.dispatch

import scala.annotation.tailrec

import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext

import akka.actor._
import akka.dispatch.sysmsg._
import akka.scalajs.jsapi.Timers

class MessageDispatcher(
    val mailboxes: Mailboxes) extends ExecutionContext {

  /**
   *  Creates and returns a mailbox for the given actor.
   */
  protected[akka] def createMailbox(actor: ActorCell): Mailbox =
    new Mailbox(new NodeMessageQueue)

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
  final def detach(actor: ActorCell): Unit = {
    unregister(actor)
    /*try unregister(actor)
    finally ifSensibleToDoSoThenScheduleShutdown()*/
  }

  /**
   * If you override it, you must call it. But only ever once. See "attach"
   * for the only invocation.
   *
   * INTERNAL API
   */
  protected[akka] def register(actor: ActorCell) {
    //addInhabitants(+1)
  }

  /**
   * If you override it, you must call it. But only ever once. See "detach"
   * for the only invocation
   *
   * INTERNAL API
   */
  protected[akka] def unregister(actor: ActorCell) {
    //addInhabitants(-1)
    val mailBox = actor.swapMailbox(mailboxes.deadLetterMailbox)
    mailBox.becomeClosed()
    mailBox.cleanUp()
  }

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
  protected[akka] def systemDispatch(receiver: ActorCell, invocation: SystemMessage) = {
    val mbox = receiver.mailbox
    mbox.systemEnqueue(receiver.self, invocation)
    registerForExecution(mbox, false, true)
  }

  /**
   * Will be called when the dispatcher is to queue an invocation for execution
   *
   * INTERNAL API
   */
  protected[akka] def dispatch(receiver: ActorCell, invocation: Envelope) = {
    val mbox = receiver.mailbox
    val s = receiver.self
    mbox.enqueue(s, invocation)
    registerForExecution(mbox, true, false)
  }

  /**
   * Suggest to register the provided mailbox for execution
   *
   * INTERNAL API
   */
  protected[akka] def registerForExecution(mbox: Mailbox,
      hasMessageHint: Boolean, hasSystemMessageHint: Boolean): Boolean = {
    if (mbox.canBeScheduledForExecution(hasMessageHint, hasSystemMessageHint)) {
      if (mbox.setAsScheduled()) {
        execute(mbox)
        true
      } else false
    } else false
  }

  // TODO make these configurable
  /**
   * INTERNAL API
   */
  protected[akka] def throughput: Int = 10

  /**
   * INTERNAL API
   */
  protected[akka] def throughputDeadlineTime: Duration =
    Duration.fromNanos(1000)

  /**
   * INTERNAL API
   */
  @inline protected[akka] final val isThroughputDeadlineTimeDefined =
    throughputDeadlineTime.toMillis > 0

  // ExecutionContext API

  override def execute(runnable: Runnable): Unit = {
    Timers.setImmediate {
      runnable.run()
    }
  }

  override def reportFailure(t: Throwable): Unit = {
    // TODO publish to even stream
    Console.err.println(s"dispatcher.reportFailure($t)")
  }

}
