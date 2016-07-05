package akka.stream.javadsl

/** Determines mode in which [[Framing]] operates. */
sealed trait FramingTruncation
object FramingTruncation {
  case object ALLOW extends FramingTruncation
  case object DISALLOW extends FramingTruncation
}
