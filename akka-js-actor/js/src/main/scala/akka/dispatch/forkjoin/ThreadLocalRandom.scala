package akka.dispatch.forkjoin

object ThreadLocalRandom {

  def apply() = java.util.concurrent.ThreadLocalRandom.current()

  def current() = java.util.concurrent.ThreadLocalRandom.current()

}
