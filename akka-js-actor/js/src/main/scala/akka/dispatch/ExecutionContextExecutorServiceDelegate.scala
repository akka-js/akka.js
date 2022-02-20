package akka.dispatch

import java.util
import java.util.concurrent.{AbstractExecutorService, TimeUnit}
import scala.concurrent.ExecutionContext

private[akka] class ExecutionContextExecutorServiceDelegate(ec: ExecutionContext) extends AbstractExecutorService {
  def awaitTermination(timeout: Long, unit: TimeUnit): Boolean = false
  def execute(runnable: Runnable): Unit = ec.execute(runnable)
  def isShutdown: Boolean = false
  def isTerminated: Boolean = false
  def shutdown(): Unit = ()
  def shutdownNow(): util.List[Runnable] = util.Collections.emptyList()
}
