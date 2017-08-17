package java.util

class SplittableRandom {
  val tlr = java.util.concurrent.ThreadLocalRandom.current()

  def nextInt(bound: Int) = tlr.nextInt(bound)

}
