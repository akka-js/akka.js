package akka.dispatch.forkjoin

abstract class ForkJoinTask[V] {

  def setRawResult(unit: Unit): Unit

  def exec(): Boolean

  def getRawResult(): Unit

}
