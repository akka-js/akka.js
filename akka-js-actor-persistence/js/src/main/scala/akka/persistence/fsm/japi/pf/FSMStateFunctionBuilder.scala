package akka.persistence.fsm.japi.pf

import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSMBase
import akka.japi.pf.FI
//import akka.japi.pf.PFBuilder
//import scala.PartialFunction

class FSMStateFunctionBuilder[S, D, E] {

  def build() =
    new PartialFunction[
      PersistentFSM.Event[D],
      PersistentFSM.State[S,D,E]
    ] {}

  def erasedEvent(eventOrType: Any,
                  dataOrType: Any,
                  predicate: FI.TypedPredicate2[_, _],
                  apply: FI.Apply2[_, _]) =
}
