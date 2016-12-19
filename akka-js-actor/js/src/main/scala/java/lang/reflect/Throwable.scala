package java.lang.reflect

class InvocationTargetException(s: String, e: Throwable) extends Error(s, e) {
  def this(s: String) = this(s, null)
  def this() = this(null, null)

  def getTargetException() = e
}
