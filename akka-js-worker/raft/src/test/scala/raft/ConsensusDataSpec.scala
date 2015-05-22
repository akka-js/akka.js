package raft

import akka.testkit._
import org.scalatest._

class ConsensusDataSpec extends RaftSpec with WordSpecLike
    with MustMatchers with BeforeAndAfterEach {

  val probe = TestProbe()
  val nodes = for (i <- List.range(0, 5)) yield TestProbe().ref
  val nextIndices = (for (i <- List.range(0, 5)) yield (TestProbe().ref, 3)).toMap
  val matchIndices = (for (i <- List.range(0, 5)) yield (TestProbe().ref, 0)).toMap

  var state: Meta = _

  val testRsm = new TotalOrdering
  val entries = Vector(Entry("a", Term(1)), Entry("b", Term(1)))
  override def beforeEach = state = Meta(Term(1), Log(nodes, entries), testRsm, nodes)

  "meta" must {
  }

  "term" must {
    "increase term monotonically" in {
      val t = Term(1)
      val t1 = t.nextTerm
      t1 must be(Term(2))
      val t2 = t1.nextTerm
      t2 must be(Term(3))
    }
    "compare to terms against each other" in {
      val t1 = Term(1)
      val t2 = Term(2)
      t1 must be < t2
    }
    "find the max term given two terms" in {
      val t1 = Term(1)
      val t2 = Term(2)
      Term.max(t1, t2) must be(t2)
      Term.max(t2, t1) must be(t2)
    }
  }

  "votes" must {
    "keep track of votes received" in {
      val v = Votes()
      val v2 = v.gotVoteFrom(probe.ref)
      v2.received must have length (1)
    }
    "check if majority votes received" in {
      val v = Votes(received = List(probe.ref, probe.ref))
      v.majority(5) must be(false)
      v.majority(3) must be(true)
    }
    "keep at most one vote for a candidate per term" in {
      val thisMeta = Meta(
        term = Term(1),
        votes = Votes(votedFor = Some(probe.ref)),
        log = Log(nodes, entries),
        nodes = nodes,
        rsm = testRsm)
      state.votes = state.votes.vote(probe.ref)
      state must be(thisMeta)
      state.votes.vote(TestProbe().ref) must be(Votes(votedFor = Some(probe.ref)))
    }
  }

  "a log" must {
    "decrement the next index for a follower if older log entries must be passed" in {
      val log = Log(entries = Vector(Entry("a", Term(1)), Entry("b", Term(2))),
        nextIndices, matchIndices, 0, 0)
      log.decrementNextFor(nextIndices.head._1).nextIndex(nextIndices.head._1) must be(2)
    }
    "set the next index for a follower based on the last log entry sent" in {
      val log = Log(entries = Vector(Entry("a", Term(1)), Entry("b", Term(2))),
        nextIndices, matchIndices, 0, 0)
      log.resetNextFor(nextIndices.head._1).nextIndex(nextIndices.head._1) must be(3)
    }
    "increase match index monotonically" in {
      val log = Log(
        entries = Vector(Entry("a", Term(1)), Entry("b", Term(2))),
        nextIndex = nextIndices,
        matchIndex = matchIndices)
      log.matchFor(matchIndices.head._1).matchIndex(matchIndices.head._1) must be(1)
    }
    "set match index to specified value" in {
      val log = Log(
        entries = Vector(Entry("a", Term(1)), Entry("b", Term(2))),
        nextIndex = nextIndices,
        matchIndex = matchIndices)
      log.matchFor(matchIndices.head._1, Some(100)).matchIndex(matchIndices.head._1) must be(100)
    }
    "override apply to initialise with appropiate next and match indices" in {
      val entries = Vector(Entry("a", Term(1)), Entry("b", Term(2)))
      val nodes = for (n <- List.range(0, 5)) yield TestProbe().ref
      Log(nodes, entries).nextIndex(nodes(0)) must be(3)
    }
  }

  "replicated state machine" must {
    "apply commands to a generic state machine" in (pending)
    "keep track of the log index of the last command applied to the state machine" in (pending)
  }

  "state factory" must {
    "create a state object from file" in (pending)
    "persist a state object to file" in (pending)
  }
}