package scala.concurrent.forkjoin

object ThreadLocalRandom {

  val current = new {
    def nextInt() = scala.util.Random.nextInt()
  }
}
