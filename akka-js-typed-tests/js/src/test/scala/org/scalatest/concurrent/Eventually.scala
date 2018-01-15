package org.scalatest.concurrent

import org.scalatest._
import exceptions.{TestFailedDueToTimeoutException, TestPendingException}
import org.scalatest.Suite.anExceptionThatShouldCauseAnAbort

import scala.annotation.tailrec
import time.{Nanosecond, Nanoseconds, Span}
import PatienceConfiguration._
import org.scalactic.source
import org.scalatest.enablers.Retrying
import org.scalatest.exceptions.StackDepthException

trait Eventually extends PatienceConfiguration {

  def eventually[T](timeout: Timeout, interval: Interval)(fun: => T)(implicit retrying: Retrying[T], pos: source.Position): T =
    eventually(fun)(PatienceConfig(timeout.value, interval.value), retrying, pos)

  def eventually[T](timeout: Timeout)(fun: => T)(implicit config: PatienceConfig, retrying: Retrying[T], pos: source.Position): T =
    eventually(fun)(PatienceConfig(timeout.value, config.interval), retrying, pos)

  def eventually[T](interval: Interval)(fun: => T)(implicit config: PatienceConfig, retrying: Retrying[T], pos: source.Position): T =
    eventually(fun)(PatienceConfig(config.timeout, interval.value), retrying, pos)

  def eventually[T](fun: => T)(implicit config: PatienceConfig, retrying: Retrying[T], pos: source.Position): T =
    retrying.retry(config.timeout, config.interval, pos)(fun)
}

object Eventually extends Eventually
