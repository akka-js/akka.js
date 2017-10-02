package java.util.concurrent

class CountDownLatch(var count: Int) {

  def await() = ()

  //this could be risky
  def await(timeout: Long, unit: TimeUnit) = true

  def countDown(): Unit = count -= 1

  def getCount(): Long = count.toLong

  override def toString(): String = s"$count"
}
