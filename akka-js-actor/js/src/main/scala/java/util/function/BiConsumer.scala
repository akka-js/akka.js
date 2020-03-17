package java.util.function

trait BiConsumer[T, U] {
  def accept(t: T, ex: Throwable): Unit
}
