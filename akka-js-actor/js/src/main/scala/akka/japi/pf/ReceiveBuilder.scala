package akka.japi.pf

import scala.runtime.BoxedUnit
import akka.actor.AbstractActor
import akka.actor.AbstractActor.Receive

object ReceiveBuilder {

  def create() : ReceiveBuilder = new ReceiveBuilder()

}

class ReceiveBuilder {

  private var statements: PartialFunction[Any, BoxedUnit] = null

  protected def addStatement(statement : PartialFunction[Any, BoxedUnit]): Unit = {
    if (statements == null)
      statements = statement
    else
      statements = statements.orElse(statement)
  }

  def build() : Receive = {
    val empty = CaseStatement.empty()

    if (statements == null)
      new Receive(empty)
    else
      new Receive(statements.orElse(empty)) // FIXME why no new Receive(statements)?
  }

}
