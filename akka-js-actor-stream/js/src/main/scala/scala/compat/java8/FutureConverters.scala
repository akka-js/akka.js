package scala.compat.java8

object FutureConverters {

  def toJava[T](f: scala.concurrent.Future[T]): java.util.concurrent.CompletionStage[T] = ???

}
