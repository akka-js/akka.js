package java.util.concurrent

import java.util.function.BiConsumer

trait CompletionStage[T] {
  def whenComplete(action: BiConsumer[_ <: T, _ <: Throwable]): CompletionStage[T]
}
