/**
 *  Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package org.scalajs.actors
package cell

import scala.concurrent.duration.{Duration, FiniteDuration}

import ReceiveTimeout.emptyReceiveTimeoutData
import util.JSTimeoutTask

private[actors] object ReceiveTimeout {
  final val emptyReceiveTimeoutData: (Duration, Cancellable) =
    (Duration.Undefined, ActorCell.emptyCancellable)
}

private[actors] trait ReceiveTimeout { this: ActorCell =>

  import ReceiveTimeout._
  import ActorCell._

  private var receiveTimeoutData: (Duration, Cancellable) =
    emptyReceiveTimeoutData

  final def receiveTimeout: Duration = receiveTimeoutData._1

  final def setReceiveTimeout(timeout: Duration): Unit =
    receiveTimeoutData = receiveTimeoutData.copy(_1 = timeout)

  final def checkReceiveTimeout() {
    val recvtimeout = receiveTimeoutData
    //Only reschedule if desired and there are currently no more messages to be processed
    if (!mailbox.hasMessages) recvtimeout._1 match {
      case f: FiniteDuration =>
        recvtimeout._2.cancel() //Cancel any ongoing future
        val task = JSTimeoutTask(f) {
          self ! ReceiveTimeout
        }
        receiveTimeoutData = (f, task)
      case _ => cancelReceiveTimeout()
    }
    else cancelReceiveTimeout()

  }

  final def cancelReceiveTimeout(): Unit = {
    if (receiveTimeoutData._2 ne emptyCancellable) {
      receiveTimeoutData._2.cancel()
      receiveTimeoutData = (receiveTimeoutData._1, emptyCancellable)
    }
  }

}
