package raft

import scala.language.postfixOps
import org.scalatest._
import akka.testkit._
import scala.concurrent.duration._

class FollowerSpec extends RaftSpec with BeforeAndAfterEach {

  var default: Meta = _
  var follower: TestFSMRef[Role, Meta, Raft] = _

  override def beforeEach = {
    default = Meta(
      term = Term(2),
      log = Log(List(probe.ref), Vector(Entry("a", Term(1)), Entry("b", Term(2)))),
      rsm = totalOrdering,
      nodes = List(probe.ref)
    )
    follower = TestFSMRef(new Raft())
  }

  val probe = TestProbe()
  val totalOrdering = new TotalOrdering

  "a follower" must {
    "not append entries if requester's term is less than current term" in {
      follower.setState(Follower, default)
      follower ! AppendEntries(
        term = Term(1),
        leaderId = testActor,
        prevLogIndex = 2,
        prevLogTerm = Term(1),
        entries = Vector(Entry("op", Term(1))),
        leaderCommit = 1)
      expectMsg(AppendFailure(Term(2)))
    }

    "not append entries if log doesn't contain an entry at " +
      "previous index" in {
        follower.setState(Follower, default)
        follower ! AppendEntries(
          term = Term(2),
          leaderId = testActor,
          prevLogIndex = 3, // this is one step ahead of follower.log.length-1
          prevLogTerm = Term(2),
          entries = Vector(Entry("op", Term(2))),
          leaderCommit = 0
        )
        expectMsg(AppendFailure(Term(2)))
      }

    "not append entries if log doesn't contain an entry at " +
      "previous index with matching terms" in {
        follower.setState(Follower, default)
        follower ! AppendEntries(
          term = Term(3),
          leaderId = testActor,
          prevLogIndex = 2, // last position in follower log
          prevLogTerm = Term(3),
          entries = Vector(Entry("op", Term(3))),
          leaderCommit = 0
        )
        expectMsg(AppendFailure(Term(3))) // TODO: needs to include jump back info
      }

    "remove uncommitted entries if appending at a position " +
      "less than log length" in {
        val longer = default.copy()
        longer.append(Vector(Entry("remove", Term(2))), 2)
        follower.setState(Follower, longer)
        follower ! AppendEntries(
          term = Term(3),
          leaderId = testActor,
          prevLogIndex = 2, // matches follower's entry ("b", 2)
          prevLogTerm = Term(2), // matches follower's entry's term
          entries = Vector(Entry("op", Term(3))),
          leaderCommit = 0
        )
        expectMsg(AppendSuccess(Term(3), 3))
        follower.stateData.log.entries must not contain (Entry("remove", Term(2)))
      }

    "append entry if previous log index and term match" in {
      follower.setState(Follower, default)
      follower ! AppendEntries(
        term = Term(3),
        leaderId = testActor,
        prevLogIndex = 2, // matches follower's last entry
        prevLogTerm = Term(2), // matches follower's last entry's term
        entries = Vector(Entry("op", Term(3))),
        leaderCommit = 0
      )
      expectMsg(AppendSuccess(Term(3), 3))
      follower.stateData.log.entries.last.command must be("op")
    }

    "append NOOP entries" in {
      follower.setState(Follower, default)
      follower ! AppendEntries(
        term = Term(3),
        leaderId = testActor,
        prevLogIndex = 2,
        prevLogTerm = Term(2),
        entries = Vector(),
        leaderCommit = 0
      )
      expectMsg(AppendSuccess(Term(3), 2))
      follower.stateData.log.entries.last.command must be("b")
    }

    "append NOOP entries when log is empty (i.e., bootstrap)" in {
      val empty = Meta(
        term = Term(1),
        log = Log(List(probe.ref), Vector()),
        rsm = totalOrdering,
        nodes = List(probe.ref)
      )
      follower.setState(Follower, empty)
      follower ! AppendEntries(
        term = Term(2),
        leaderId = testActor,
        prevLogIndex = 0,
        prevLogTerm = Term(1),
        entries = Vector(),
        leaderCommit = 0
      )
      expectMsg(AppendSuccess(Term(2), 0))
    }

    "append NOOP entries when log is empty in same term" in {
      val empty = Meta(
        term = Term(50),
        log = Log(List(probe.ref), Vector()),
        rsm = totalOrdering,
        nodes = List(probe.ref)
      )
      follower.setState(Follower, empty)
      follower ! AppendEntries(
        term = Term(50),
        leaderId = testActor,
        prevLogIndex = 0,
        prevLogTerm = Term(1),
        entries = Vector(),
        leaderCommit = 0
      )
      expectMsg(AppendSuccess(Term(50), 0))
    }

    "append multiple entries if previous log index and term match" in {
      follower.setState(Follower, default)
      val probe = TestProbe()
      probe.send(follower, AppendEntries(
        term = Term(3),
        leaderId = testActor,
        prevLogIndex = 2, // matches follower's last entry
        prevLogTerm = Term(2), // matches follower's last entry's term
        entries = Vector(Entry("op", Term(3)), Entry("op2", Term(3))),
        leaderCommit = 0
      ))
      probe.expectMsg(AppendSuccess(Term(3), 4))
      follower.stateData.log.entries must contain(Entry("op", Term(3)))
      follower.stateData.log.entries.last must be(Entry("op2", Term(3)))
    }

    "apply committed entries" in {
      follower.setState(Follower, default)
      follower.stateData.log.lastApplied must be(0)
      val probe = TestProbe()
      probe.send(follower, AppendEntries(
        term = Term(3),
        leaderId = testActor,
        prevLogIndex = 2, // matches follower's last entry
        prevLogTerm = Term(2), // matches follower's last entry's term
        entries = Vector(Entry("op", Term(3))),
        leaderCommit = 2 // safe to commit and apply log entry 1 and 2
      ))
      follower.stateData.log.lastApplied must be(2)
    }

    /* ---------- VOTING -----------*/

    "grant vote if candidate term is higher to own term" in {
      follower.setState(Follower, default)
      follower ! RequestVote(Term(3), testActor, 2, Term(2))
      expectMsg(GrantVote(Term(3)))
    }

    "grant vote if candidate is exactly equal" in {
      follower.setState(Follower, default)
      follower ! RequestVote(
        term = Term(2),
        candidateId = testActor,
        lastLogIndex = 1,
        lastLogTerm = Term(2)
      )
      expectMsg(GrantVote(Term(2)))
    }

    "deny vote if own log's last term is more up to date than candidate" in {
      //      If the logs have last entries with different terms
      //      then the log with the later term is more up to date. 
      //      If the logs end with the same term, then whichever log is 
      //      longer (i.e., logIndex) is more up to date.
      follower.setState(Follower, default)
      follower ! RequestVote(
        term = Term(2),
        candidateId = testActor,
        lastLogIndex = 1,
        lastLogTerm = Term(1)
      )
      expectMsg(DenyVote(Term(2)))
    }

    "deny vote if own log's last term is equal but log is longer than candidate" in {
      //      If the logs have last entries with different terms
      //      then the log with the later term is more up to date. 
      //      If the logs end with the same term, then whichever log is 
      //      longer (i.e., logIndex) is more up to date.
      val longer = default.copy()
      longer.append(Vector(Entry("c", Term(2))), 2)
      follower.setState(Follower, longer)
      follower ! RequestVote(
        term = Term(2),
        candidateId = testActor,
        lastLogIndex = 1, // shorter log than follower
        lastLogTerm = Term(2)
      )
      expectMsg(DenyVote(Term(2)))
    }

    "deny vote if term is lower than own term" in {
      follower.setState(Follower, default)
      follower ! RequestVote(Term(1), testActor, 2, Term(2))
      expectMsg(DenyVote(Term(2)))
    }

    "deny vote if vote for term already cast in same term" in {
      val voted = default.copy(votes = default.votes.vote(probe.ref))
      follower.setState(Follower, voted)
      follower ! RequestVote(Term(2), testActor, 2, Term(2))
      expectMsg(DenyVote(Term(2)))
    }

    "convert to candidate if no messages are received within timeout" in {
      follower.setState(Follower, default)
      follower.setTimer("timeout", Timeout, 1 millis, false)
      Thread.sleep(40)
      follower.stateName must be(Candidate)
    }

    "reset timer when granting vote" in {
      follower.setState(Follower, default)
      follower.setTimer("timeout", Timeout, 200 millis, false)
      Thread.sleep(150)
      follower ! RequestVote(Term(3), testActor, 2, Term(2)) // follower grants vote
      Thread.sleep(100) // ensures first timeout has expired 
      follower.stateName must be(Follower) // stays as follower
      follower.isTimerActive("timeout") must be(true)

    }

    "convert to candidate if timeout reached" in {
      follower.setState(Follower, default)
      follower.setTimer("timeout", Timeout, 200 millis, false)
      Thread.sleep(150)
      follower ! RequestVote(Term(1), testActor, 2, Term(2)) // follower denies this
      Thread.sleep(100) // ensures first timeout has expired 
      follower.stateName must be(Candidate) // timeout happened,
      // transition to candidate
    }

    "reset timeout after receiving AppendEntriesRPC" in {
      follower.setState(Follower, default)
      follower.setTimer("timeout", Timeout, 200 millis, false)
      Thread.sleep(150)
      follower ! AppendEntries(Term(3), testActor, 1, Term(2), Vector(Entry("op", Term(3))), 0)
      Thread.sleep(100) // ensures first timeout has expired 
      follower.stateName must be(Follower) // stays as follower
      follower.isTimerActive("timeout") must be(true)
    }

    "increase its term when transitioning to candidate" in {
      follower.setState(Follower, default)
      follower.stateData.term.current must be(2)
      follower.setTimer("timeout", Timeout, 1 millis, false)
      Thread.sleep(30)
      follower.stateData.term.current must be(3) // is candidate by now
    }

    "increase own term if RequestVote contains higher term" in {
      follower.setState(Follower, default)
      follower ! RequestVote(Term(3), testActor, 1, Term(1)) // higher term
      follower.stateName must be(Follower)
      follower.stateData.term.current must be(3)
    }

    "increase own term if AppendEntries contains higher term" in {
      follower.setState(Follower, default)
      follower ! AppendEntries(Term(3), testActor, 2, Term(2), Vector(Entry("op", Term(3))), 0) // higher term
      follower.stateName must be(Follower)
      follower.stateData.term.current must be(3)
    }

    "set leader when receiving append entries" in {
      follower.setState(Follower, default)
      follower ! AppendEntries(Term(3), testActor, 2, Term(2),
        Vector(Entry("op", Term(3))), 0)
      follower.stateData.leader must be(Some(testActor))
    }

    "forward client requests to leader" in {
      val probe = TestProbe()
      val withLeader = default.copy(leader = Some(probe.ref))
      follower.setState(Follower, withLeader)
      val request = ClientRequest(100, "get")
      follower ! request
      probe.expectMsg(request)
    }
  }
}