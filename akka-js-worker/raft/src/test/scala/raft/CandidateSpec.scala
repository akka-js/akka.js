package raft

import scala.language.postfixOps
import org.scalatest._
import akka.testkit._
import akka.actor.ActorDSL._
import scala.concurrent.duration._

class CandidateSpec extends RaftSpec with BeforeAndAfterEach {

  var initialCandidateState: Meta = _
  var candidate: TestFSMRef[Role, Meta, Raft] = _

  def probes(size: Int) = (for (i <- List.range(0, size)) yield TestProbe())
  val totalOrdering = new TotalOrdering

  override def beforeEach = {
    candidate = TestFSMRef(new Raft())
    val allNodes = testActor :: candidate :: probes(3).map(_.ref)
    initialCandidateState = Meta(
      term = Term(3),
      log = Log(allNodes, Vector(Entry("a", Term(1)), Entry("b", Term(2)))),
      rsm = totalOrdering,
      nodes = allNodes
    )
  }

  "when converting to a candidate it" must {
    "increase its term" in {
      candidate.setState(Follower, initialCandidateState) // reusing state
      candidate.setTimer("timeout", Timeout, 1 millis, false) // force transition
      Thread.sleep(40) // ensure timeout elapses
      candidate.stateData.term.current must be(4)
    }

    "vote for itself" in {
      candidate.setState(Follower, initialCandidateState) // reusing state
      candidate.setTimer("timeout", Timeout, 1 millis, false) // force transition
      Thread.sleep(40) // ensure timeout elapses
      candidate.stateData.votes.received must contain(candidate)
    }

    "reset election timeout" in {
      candidate.setState(Follower, initialCandidateState) // reusing state
      candidate.setTimer("force transition", Timeout, 1 millis, false)
      Thread.sleep(40) // ensure timeout elapses
      candidate.isTimerActive("timeout") must be(true) // check that default timer is set
    }

    "request votes from all other servers" in {
      val myprobes = probes(5)
      initialCandidateState.nodes = myprobes.map(_.ref)
      candidate.setState(Follower, initialCandidateState) // reusing state
      candidate.setTimer("timeout", Timeout, 1 millis, false) // force transition
      Thread.sleep(40) // ensure timeout elapses
      myprobes.map(_.expectMsg(RequestVote(Term(4), candidate, 2, Term(2))))
    }
  }

  "a candidate" must {
    "become leader if receiving grants from a majority of servers" in {
      candidate.setState(Candidate, initialCandidateState)

      for (i <- List.range(0, 4)) // excluding candidate  
        yield actor(new Act {
        whenStarting { candidate ! GrantVote(Term(3)) }
      })

      Thread.sleep(50) // give candidate time to receive and parse messages
      candidate.stateName must be(Leader)
    }

    "remain a candidate if the majority vote is not yet received" in {
      val cand = TestFSMRef(new Raft())
      cand.setState(Candidate, initialCandidateState)
      for (i <- List.range(0, 2)) // yields two votes  
        yield actor(new Act {
        whenStarting { cand ! GrantVote(Term(3)) }
      })
      Thread.sleep(50)
      cand.stateName must be(Candidate)
    }

    "ensure that receiving grant does not trigger new request vote" in {
      val probe = TestProbe()
      val cand = TestFSMRef(new Raft())
      cand.setState(Candidate, initialCandidateState)
      probe.send(cand, GrantVote(Term(3)))
      probe.expectNoMsg
    }

    "convert to follower if receiving append entries message from new leader" in {
      candidate.setState(Candidate, initialCandidateState)
      candidate ! AppendEntries(
        term = Term(4),
        leaderId = testActor,
        prevLogIndex = 3,
        prevLogTerm = Term(2),
        entries = Vector(Entry("op", Term(2))),
        leaderCommit = 0
      )
      candidate.stateName must be(Follower)
    }

    "start a new election if timeout elapses" in {
      candidate.setState(Candidate, initialCandidateState)
      candidate.setTimer("timeout", Timeout, 1 millis, false) // force transition
      Thread.sleep(40) // ensure timeout elapses
      // i.e., no messages are received within the timeout 
      candidate.stateName must be(Candidate)
      candidate.stateData.term.current must be(4) // since initial was 3
    }

    "set leader when receiving append entries" in {
      candidate.setState(Follower, initialCandidateState)
      candidate ! AppendEntries(
        term = Term(4),
        leaderId = testActor,
        prevLogIndex = 3,
        prevLogTerm = Term(2),
        entries = Vector(Entry("op", Term(2))),
        leaderCommit = 0
      )
      candidate.stateData.leader must be(Some(testActor))
    }
  }
}