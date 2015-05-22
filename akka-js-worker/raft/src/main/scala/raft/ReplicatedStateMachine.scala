package raft

import scala.language.higherKinds
import scala.util.Try

abstract class ReplicatedStateMachine[C[_]] {
  def execute[T](command: C[T]): T
}

sealed trait Command[T]
case object Get extends Command[Int]

class TotalOrdering extends ReplicatedStateMachine[Command] {
  private var state: Int = 0
  override def execute[T](command: Command[T]): T = command match {
    case Get => state += 1; state
  }
}
