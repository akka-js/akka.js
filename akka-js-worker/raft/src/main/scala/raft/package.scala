
import scala.language.implicitConversions
import akka.actor.ActorRef

package object raft {
  /* raft types */
  type NodeId = ActorRef

  /* converters */
  implicit def canBuildFrom(log: Vector[Entry]) = new InMemoryEntries(log)
}