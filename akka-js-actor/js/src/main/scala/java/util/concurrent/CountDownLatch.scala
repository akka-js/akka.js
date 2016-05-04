package java.util.concurrent

class CountDownLatch(var count: Int) {

  def await() = ()

  //def boolean await(long timeout, TimeUnit unit)

  def countDown(): Unit = count -= 1

  def getCount(): Long = count.toLong

  override def toString(): String = s"$count"
}
