package raft

import org.scalatest._

class ReplicatedStateMachineSpec extends WordSpecLike
    with MustMatchers with BeforeAndAfterEach {

  var rsm: TotalOrdering = _

  override def beforeEach = rsm = new TotalOrdering()

  "a replicated state machine" must {
    "increment counter on get" in {
      rsm.execute(Get) must be(1)
      rsm.execute(Get) must be(2)
      rsm.execute(Get) must be(3)
    }
  }
}