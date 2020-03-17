package java.util.concurrent

trait ExecutorService extends Executor {
  def shutdown(): Unit
}
