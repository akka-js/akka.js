package akka.dispatch.forkjoin

import scala.concurrent.Future

abstract class ForkJoinTask[V] {

  def setRawResult(unit: Unit): Unit

  def exec(): Boolean

  def getRawResult(): Unit

  //here instead of IR patches
  var unsafe: Array[AnyRef] = null
}
