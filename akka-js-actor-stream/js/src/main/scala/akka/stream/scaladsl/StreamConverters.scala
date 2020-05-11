/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.scaladsl

import java.io.{ InputStream, OutputStream }
import java.util.Spliterators
import java.util.stream.{ Collector, StreamSupport }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.concurrent.duration.Duration._

import akka.NotUsed
import akka.stream.{ Attributes, IOResult, SinkShape }
import akka.stream.impl._
import akka.stream.impl.Stages.DefaultAttributes
// import akka.stream.impl.io.{ InputStreamSinkStage, InputStreamSource, OutputStreamGraphStage, OutputStreamSourceStage }
import akka.util.ByteString

/**
 * Converters for interacting with the blocking `java.io` streams APIs and Java 8 Streams
 */
object StreamConverters {

  def fromInputStream(in: () => InputStream, chunkSize: Int = 8192): Source[ByteString, Future[IOResult]] = ???

  def asOutputStream(writeTimeout: FiniteDuration = 5.seconds): Source[ByteString, OutputStream] = ???

  def fromOutputStream(out: () => OutputStream, autoFlush: Boolean = false): Sink[ByteString, Future[IOResult]] = ???

  def asInputStream(readTimeout: FiniteDuration = 5.seconds): Sink[ByteString, InputStream] = ???

  def javaCollector[T, R](collectorFactory: () => java.util.stream.Collector[T, _ <: Any, R]): Sink[T, Future[R]] = ???

  def javaCollectorParallelUnordered[T, R](parallelism: Int)(
      collectorFactory: () => java.util.stream.Collector[T, _ <: Any, R]): Sink[T, Future[R]] = ???

  def asJavaStream[T](): Sink[T, java.util.stream.Stream[T]] = ???

  def fromJavaStream[T, S <: java.util.stream.BaseStream[T, S]](
      stream: () => java.util.stream.BaseStream[T, S]): Source[T, NotUsed] = ???
}
