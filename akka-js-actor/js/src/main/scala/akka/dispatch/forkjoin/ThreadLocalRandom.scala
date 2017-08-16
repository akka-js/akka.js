package akka.dispatch.forkjoin

object ThreadLocalRandom {

  def apply() = java.util.concurrent.ThreadLocalRandom.current()

}
