//package raft 
//
//import akka.actor.{ Actor, ActorRef, FSM }
//import scala.concurrent.duration._
//
///* Types */
//object Raft {
//  type Term = Int
//  type NodeId = Int
//}
//
///* Reply events */
//sealed trait Reply
//case class VoteReply(term: Raft.Term, voteGranted: Boolean) extends Reply
//case class AppendEntriesReply(term: Raft.Term, success: Boolean) extends Reply
//
///* Request events */
//sealed trait Request
//case class AppendEntries(term: Raft.Term, leaderId: Raft.NodeId, 
//  prevLogIndex: Int, prevLogTerm: Raft.Term, 
//  entries: Log, leaderCommit: Int) extends Request
//case class RequestVote(term: Raft.Term, candidateId: Raft.NodeId, 
//  lastLogIndex: Int, lastLogTerm: Raft.Term) extends Request
//
///** 
// * Log
// *
// * This should eventually be persistent
// * Move to separate file
// */
//case class Log(entries: List[(Raft.Term, String)])
//
///* states */
//sealed trait Role
//case object Leader extends Role 
//case object Follower extends Role
//case object Candidate extends Role
//
///* state data */
//sealed trait Data
//case class Meta(stored: PersistentData, memory: VolatileData) extends Data
//case object Unintitialized extends Data
//
//case class PersistentData(currentTerm: Raft.Term, 
//  var votedFor: Option[Raft.NodeId], log: Log)
//
//sealed trait VolatileData
//case class ServerData(commitIndex: Int, 
//  lastApplied: Int) extends VolatileData
//case class LeaderServerData(commitIndex: Int, 
//  lastApplied: Int,
//  var nextIndex: Map[Raft.NodeId, Int],
//  var matchIndex: Map[Raft.NodeId, Int]) extends VolatileData
//
///* Consensus module */
//class Raft(var nodes: List[String]) extends Actor with FSM[Role, Data] {
//  startWith(Candidate, Unintitialized)
///*
//  when(Follower, stateTimeout = 1 second) {
//    // respond to RPCs from candiate and leaders
//    case Event(ae: AppendEntries, m: Meta) =>
//      val updatedMeta = handleAppendEntries(ae, m)
//      stay using updatedMeta
//
//    case Event(requestVote: RequestVote, m: Meta) =>
//      updatedMeta = vote(requestVote, m) match {
//        case true =>
//          source ! VoteReply(m.currentTerm, true)
//        case false =>
//          source ! VoteReply(m.currentTerm, false)
//      }
//
//    // if election timeout w/o AppendEntries from current leader 
//    //  or granting vote from candidate - convert to candidate
//    case Event(StateTimeout, m: Meta) =>
//      goto(Candidate) using m // might have to modify m
//  }
//  
//  when(Candidate) {
//    // if votes received from majority
//    //  become leader
//    case Event(vote: VoteReply, m: Meta) =>
//      val updatedMeta = m.copy(votes = vote :: m.votes)
//      if majority(updatedMeta) goto(Leader) using updatedMeta
//      else stay using updatedMeta
//    
//    case Event(ae: AppendEntries, m: Meta) =>
//      // if AppendEntries from new leader
//      //  become follower
//      if (ae.leaderId == ae.source) 
//        goto(Follower) using m // might have to modify m
//
//    // if election timeout elapses
//    //  start new election
//    case Event(StateTimeout, m: Meta) =>
//      goto(Candidate) using m // might have to modify m
//  }
//
//  when(Leader) {
//    // has nextIndex[] - next log index for each server (init = leader last log index + 1)
//    // has matchIndex[] - heighest index known to be replicated for each server
//    // 
//    // upon election, send heartbeat
//    // repeat heartbeat during idle periods
//    // receive command from client
//    //  append entry to local log
//    //  respond to client after entry applied to state
//    // if last log index >= nextIndex for a follower:
//    //  send AppendEntries with log entries starting at nextIndex
//    //  if successful:
//    //    update nextIndex and matchIndex for follower
//    //  if failure:
//    //    decrement nextIndex
//    // if there exists an N such that N > commitIndex, a majority of matchIndex[i] >= N
//    //  and log[N].term == currentTerm
//    //  set commitIndex = N
//  }
//
//  whenUnhandled {
//    // #persistent
//    //  currentTerm - increases monotonically, init=0
//    //  votedFor - candidateId voted for in term, init=Option[ActorRef]
//    //  log[[{term, command}]
//
//    // #volatile 
//    //  commitIndex - index of heighest log entry, init = 0
//    //  lastApplied - index of heighest log entry applied to state machine, init=0
//
//    // if commitIndex > lastApplied:
//    //   increment lastApplied
//    //   apply log[lastApplied] to state machine
//    // if RPC request or response contains term > currentTerm:
//    //  set currentTerm = T, convert to follower
//  }
//
//  onTransition {
//    case (Follower | Leader) -> Candidate =>
//      // increase term
//      // vote for self
//      // reset election timeout
//      // send request vote to all servers
//  }
//*/
//  initialize()
//}
