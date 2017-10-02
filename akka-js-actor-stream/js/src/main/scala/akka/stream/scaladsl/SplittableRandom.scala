package akka.stream.scaladsl

import java.util.Random
import java.util.{SplittableRandom => juSplittableRandom}

class SplittableRandom {

  private val inner = new juSplittableRandom()
  private val plainRandom = new java.util.Random()

  def nextInt(bound: Int): Int = {
    plainRandom.nextInt(bound)
  }
}
