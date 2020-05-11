package akka.stream.javadsl

import java.io.{ InputStream, OutputStream }
import java.util.concurrent.CompletionStage
import java.util.stream.Collector

import scala.concurrent.duration.FiniteDuration

import com.github.ghik.silencer.silent

import akka.NotUsed
import akka.japi.function
import akka.stream.{ javadsl, scaladsl }
import akka.stream.IOResult
import akka.util.ByteString

object StreamConverters {

  def fromOutputStream(f: function.Creator[OutputStream]): javadsl.Sink[ByteString, CompletionStage[IOResult]] = ???

  def fromOutputStream(
      f: function.Creator[OutputStream],
      autoFlush: Boolean): javadsl.Sink[ByteString, CompletionStage[IOResult]] = ???

  def asInputStream(): Sink[ByteString, InputStream] = ???

  def asInputStream(readTimeout: FiniteDuration): Sink[ByteString, InputStream] = ???

  def asInputStream(readTimeout: java.time.Duration): Sink[ByteString, InputStream] = ???

  def fromInputStream(
      in: function.Creator[InputStream],
      chunkSize: Int): javadsl.Source[ByteString, CompletionStage[IOResult]] = ???

  def fromInputStream(in: function.Creator[InputStream]): javadsl.Source[ByteString, CompletionStage[IOResult]] = ???

  def asOutputStream(writeTimeout: FiniteDuration): javadsl.Source[ByteString, OutputStream] = ???

  def asOutputStream(writeTimeout: java.time.Duration): javadsl.Source[ByteString, OutputStream] = ???

  def asOutputStream(): javadsl.Source[ByteString, OutputStream] = ???

  def asJavaStream[T](): Sink[T, java.util.stream.Stream[T]] = ???

  def fromJavaStream[O, S <: java.util.stream.BaseStream[O, S]](
      stream: function.Creator[java.util.stream.BaseStream[O, S]]): javadsl.Source[O, NotUsed] = ???

  def javaCollector[T, R](collector: function.Creator[Collector[T, _ <: Any, R]]): Sink[T, CompletionStage[R]] = ???

  def javaCollectorParallelUnordered[T, R](parallelism: Int)(
      collector: function.Creator[Collector[T, _ <: Any, R]]): Sink[T, CompletionStage[R]] = ???

}
