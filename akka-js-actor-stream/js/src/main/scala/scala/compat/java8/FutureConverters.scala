package scala.compat.java8

import scala.concurrent.Future
import java.util.concurrent.CompletionStage

object FutureConverters {

  def toJava[T](f: Future[T]): CompletionStage[T] = ???
  def toScala[T](cs: CompletionStage[T]): Future[T] = ???

  implicit def FutureOps[T](f: Future[T]): FutureOps[T] = new FutureOps[T](f)
  final class FutureOps[T](val __self: Future[T]) extends AnyVal {
    def toJava: CompletionStage[T] = FutureConverters.toJava(__self)
  }

  implicit def CompletionStageOps[T](cs: CompletionStage[T]): CompletionStageOps[T] = new CompletionStageOps(cs)

  final class CompletionStageOps[T](val __self: CompletionStage[T]) extends AnyVal {
    def toScala: Future[T] = FutureConverters.toScala(__self)
  }

}
