package raft

import scala.language.postfixOps
import akka.actor.{ Actor, ActorRef/*, FSM, LoggingFSM*/ }
import scala.concurrent.duration._
import scala.concurrent.Promise
import math.random
import akka.actor.ActorSystem
import akka.actor.Props
import akka.worker._

/* messages */
sealed trait Message
case object Timeout extends Message
case object Heartbeat extends Message
case class Init(nodes: List[NodeId]) extends Message

case class RequestVote(
  term: Term,
  candidateId: NodeId,
  lastLogIndex: Int,
  lastLogTerm: Term) extends Message

case class AppendEntries(
  term: Term,
  leaderId: NodeId,
  prevLogIndex: Int,
  prevLogTerm: Term,
  entries: Vector[Entry],
  leaderCommit: Int) extends Message

sealed trait Vote extends Message
case class DenyVote(term: Term) extends Vote
case class GrantVote(term: Term) extends Vote

sealed trait AppendReply extends Message
case class AppendFailure(term: Term) extends AppendReply
case class AppendSuccess(term: Term, index: Int) extends AppendReply

case class ClientRequest(cid: Int, command: String) extends Message


/* Consensus module */
class Raft(val i: Int, val ui: ActorRef) extends Actor /*with LoggingFSM[Role, Meta]*/ {
  import scala.scalajs.js.timers._
  import scala.collection.mutable.HashMap
  
  //override def logDepth = 12

  var data: Meta = Meta(List())
  var state: Role = Initialise
  val timers = HashMap[String, SetTimeoutHandle]()
  
  def cancelTimer(name: String) = {
    try {
      clearTimeout(timers(name)) 
    } catch { case _ => }
  }
  
  def setTimer(name: String, kind: Message, timeout: FiniteDuration, bool: Boolean) = 
    timers += name -> setTimeout(timeout) { self ! kind }
  
  def switchState(to: Role) = {
    (state, to) match {
      case (Leader, Follower) =>
        cancelTimer("heartbeat")
        resetTimer
      case (Candidate, Follower) => resetTimer
      case (Initialise, Follower) => resetTimer
      case _ => 
    }
    state = to
    ui ! UIState(i, state)
    context.become(to match {
      case Follower => follower
      case Candidate => candidate
      case Leader => leader
    })
  }
  
  def receive = {
    case cluster: Init =>
      data = initialised(cluster)
      switchState(Follower)
  }
  
  def follower: Receive = {
    case rpc: RequestVote =>
      vote(rpc, data) match {
        case (msg: GrantVote, updData) =>
          resetTimer
          data = updData
          sender ! msg
        case (msg: DenyVote, updData) =>
          data = updData 
          sender ! msg
      }
    case rpc: AppendEntries =>
      data.setLeader(rpc.leaderId)
      resetTimer
      val msg = append(rpc, data)
      sender ! msg
    case rpc: ClientRequest =>
      forwardRequest(rpc, data)
    case Timeout =>
      data = preparedForCandidate(data)
      switchState(Candidate)
  }
  
  def candidate: Receive = {
    // voting events   
    case GrantVote(term) =>
      data.votes = data.votes.gotVoteFrom(sender)
      if (data.votes.majority(data.nodes.length))
        data = preparedForLeader(data)
        switchState(Leader)
    case DenyVote(term) =>
      if (term > data.term) {
        data.selectTerm(term)
        data = preparedForFollower(data)
        switchState(Follower)
      } 

    // other   
    case rpc: AppendEntries =>
      data.setLeader(rpc.leaderId)
      val msg = append(rpc, data)
      data = preparedForFollower(data)
      sender ! msg
      switchState(Follower)
    case rpc: ClientRequest =>
      forwardRequest(rpc, data)
    case Timeout =>
      data = preparedForCandidate(data)
      switchState(Candidate)
  }    
  
  def leader: Receive = {
    case clientRpc: ClientRequest =>
      writeToLog(sender, clientRpc, data)
      sendEntries(data)
    case rpc: AppendSuccess =>
      data.log = data.log.resetNextFor(sender)
      data.log = data.log.matchFor(sender, Some(rpc.index))
      leaderCommitEntries(rpc, data)
      applyEntries(data)
    case rpc: AppendFailure =>
      if (rpc.term <= data.term) {
        data.log = data.log.decrementNextFor(sender)
        resendTo(sender, data) // let heartbeats do the catch up work
      } else {
        data.term = rpc.term
        data = preparedForFollower(data)
        switchState(Follower)
      }
    case Heartbeat =>
      ui ! UIHeartbeat(i)
      sendEntries(data)
  }
  
  /*
  startWith(Initialise, Meta(List()))

  when(Initialise) {
    case Event(cluster: Init, _) => goto(Follower) using initialised(cluster)
  }

  when(Follower) {
    case Event(rpc: RequestVote, data) =>
      vote(rpc, data) match {
        case (msg: GrantVote, updData) =>
          resetTimer
          stay using (updData) replying (msg)
        case (msg: DenyVote, updData) =>
          stay using (updData) replying (msg)
      }
    case Event(rpc: AppendEntries, data) =>
      data.setLeader(rpc.leaderId)
      resetTimer
      val msg = append(rpc, data)
      stay using data replying msg
    case Event(rpc: ClientRequest, data) =>
      forwardRequest(rpc, data)
      stay
    case Event(Timeout, data) =>
      goto(Candidate) using preparedForCandidate(data)
  }

  when(Candidate) {
    // voting events   
    case Event(GrantVote(term), data: Meta) =>
      data.votes = data.votes.gotVoteFrom(sender)
      if (data.votes.majority(data.nodes.length))
        goto(Leader) using preparedForLeader(data)
      else stay using data
    case Event(DenyVote(term), data: Meta) =>
      if (term > data.term) {
        data.selectTerm(term)
        goto(Follower) using preparedForFollower(data)
      } else stay

    // other   
    case Event(rpc: AppendEntries, data: Meta) =>
      data.setLeader(rpc.leaderId)
      val msg = append(rpc, data)
      goto(Follower) using preparedForFollower(data) replying msg
    case Event(rpc: ClientRequest, data) =>
      forwardRequest(rpc, data)
      stay
    case Event(Timeout, data: Meta) =>
      goto(Candidate) using preparedForCandidate(data)
  }

  when(Leader) {
    case Event(clientRpc: ClientRequest, data: Meta) =>
      writeToLog(sender, clientRpc, data)
      sendEntries(data)
      stay using data
    case Event(rpc: AppendSuccess, data: Meta) =>
      data.log = data.log.resetNextFor(sender)
      data.log = data.log.matchFor(sender, Some(rpc.index))
      leaderCommitEntries(rpc, data)
      applyEntries(data)
      stay
    case Event(rpc: AppendFailure, data: Meta) =>
      if (rpc.term <= data.term) {
        data.log = data.log.decrementNextFor(sender)
        //resendTo(sender, data) // let heartbeats do the catch up work
        stay
      } else {
        data.term = rpc.term
        goto(Follower) using preparedForFollower(data)
      }
    case Event(Heartbeat, data: Meta) =>
      sendEntries(data)
      stay
  }



  onTermination {
    case StopEvent(FSM.Failure(cause), state, data) =>
      val lastEvents = getLog.mkString("\n\t")
      log.warning(s"Failure in state $state with data $data due to $cause" +
        "Events leading up to this: \n\t$lastEvents")
  }*/

  private def preparedForFollower(state: Meta): Meta = {
    state.votes = Votes()
    state
  }

  private def preparedForCandidate(data: Meta): Meta = {
    data.nextTerm
    data.votes = Votes(votedFor = Some(self), received = List(self))
    data.nodes.filter(_ != self).map { t =>
      t ! RequestVote(
        term = data.term,
        candidateId = self,
        lastLogIndex = data.log.entries.lastIndex,
        lastLogTerm = data.log.entries.lastTerm)
    }
    resetTimer
    data
  }

  private def preparedForLeader(state: Meta) = {
    //log.info(s"Elected to leader for term: ${state.term}")
    val nexts = state.log.nextIndex.map(x => (x._1, state.log.entries.lastIndex + 1))
    val matches = state.log.matchIndex.map(x => (x._1, 0))
    state.log = state.log.copy(nextIndex = nexts, matchIndex = matches)
    sendEntries(state)
    state
  }

  private def initialised(cluster: Init): Meta = Meta(cluster.nodes)

  private def resetHeartbeatTimer = {
    cancelTimer("heartbeat")
    val nextTimeout = (random * 100).toInt + 100
    setTimer("heartbeat", Heartbeat, nextTimeout millis, false)
  }

  private def resetTimer = {
    cancelTimer("timeout")
    val nextTimeout = (random * 100).toInt + 200
    setTimer("timeout", Timeout, nextTimeout millis, false)
  }

  //initialize() // akka specific

  /*
   *  --- Internals ---
   */

  private def forwardRequest(rpc: ClientRequest, data: Meta) = {
    data.leader match {
      case Some(target) => target forward rpc
      case None => // drops message, relies on client to retry
    }
  }

  private def applyEntries(data: Meta) =
    for (i <- data.log.lastApplied until data.log.commitIndex) {
      val entry = data.log.entries(i)
      val result = data.rsm.execute(Get) // TODO: make generic
      data.log = data.log.applied

      entry.client match {
        case Some(ref) => ref.sender ! (ref.cid, result)
        case None => // ignore
      }
    }

  private def leaderCommitEntries(rpc: AppendSuccess, data: Meta) = {
    if (rpc.index >= data.log.commitIndex &&
      data.log.entries.termOf(rpc.index) == data.term) {
      val matches = data.log.matchIndex.count(_._2 == rpc.index)
      if (matches >= Math.ceil(data.nodes.length / 2.0))
        data.log = data.log.commit(rpc.index)
    }
  }

  private def sendEntries(data: Meta) = {
    resetHeartbeatTimer
    data.nodes.filterNot(_ == self).map { node =>
      val message = compileMessage(node, data)
      node ! message
    }
  }

  private def resendTo(node: NodeId, data: Meta) = {
    val message = compileMessage(node, data)
    node ! message
  }

  private def compileMessage(node: ActorRef, data: Meta): AppendEntries = {
    val prevIndex = data.log.nextIndex(node) - 1
    val prevTerm = data.log.entries.termOf(prevIndex)
    val fromMissing = missingRange(data.log.entries.lastIndex, prevIndex)
    AppendEntries(
      term = data.term,
      leaderId = self,
      prevLogIndex = prevIndex,
      prevLogTerm = prevTerm,
      entries = data.log.entries.takeRight(fromMissing),
      leaderCommit = data.log.commitIndex
    )
  }

  private def missingRange(lastIndex: Int, prevIndex: Int) =
    if (prevIndex == 0) 1
    else lastIndex - prevIndex

  private def writeToLog(sender: NodeId, rpc: ClientRequest, data: Meta) = {
    val ref = InternalClientRef(sender, rpc.cid)
    val entry = Entry(rpc.command, data.term, Some(ref))
    data.leaderAppend(self, Vector(entry))
  }

  /*
   * AppendEntries handling 
   */
  private def append(rpc: AppendEntries, data: Meta): AppendReply = {
    if (leaderIsBehind(rpc, data)) appendFail(rpc, data)
    else if (!hasMatchingLogEntryAtPrevPosition(rpc, data)) appendFail(rpc, data)
    else appendSuccess(rpc, data)
  }

  private def leaderIsBehind(rpc: AppendEntries, data: Meta): Boolean =
    rpc.term < data.term

  private def hasMatchingLogEntryAtPrevPosition(
    rpc: AppendEntries, data: Meta): Boolean =
    (rpc.prevLogIndex == 0 || // guards for bootstrap case
      (data.log.entries.hasEntryAt(rpc.prevLogIndex) &&
        (data.log.entries.termOf(rpc.prevLogIndex) == rpc.prevLogTerm)))

  private def appendFail(rpc: AppendEntries, data: Meta) = {
    data.selectTerm(rpc.term)
    AppendFailure(data.term)
  }

  private def appendSuccess(rpc: AppendEntries, data: Meta) = {
    data.append(rpc.entries, rpc.prevLogIndex)
    data.log = data.log.commit(rpc.leaderCommit)
    followerApplyEntries(data)
    data.selectTerm(rpc.term)
    AppendSuccess(data.term, data.log.entries.lastIndex)
  }

  private def followerApplyEntries(data: Meta) =
    for (i <- data.log.lastApplied until data.log.commitIndex) {
      val entry = data.log.entries(i)
      data.rsm.execute(Get) // TODO: make generic
      data.log = data.log.applied
    }

  /*
   * Determine whether to grant or deny vote
   */
  private def vote(rpc: RequestVote, data: Meta): (Vote, Meta) =
    if (alreadyVoted(rpc, data)) deny(rpc, data)
    else if (rpc.term < data.term) deny(rpc, data)
    else if (rpc.term == data.term)
      if (candidateLogTermIsBehind(rpc, data)) deny(rpc, data)
      else if (candidateLogTermIsEqualButHasShorterLog(rpc, data)) deny(rpc, data)
      else grant(rpc, data) // follower and candidate are equal, grant
    else grant(rpc, data) // candidate is ahead, grant

  private def deny(rpc: RequestVote, data: Meta) = {
    data.term = Term.max(data.term, rpc.term)
    (DenyVote(data.term), data)
  }

  private def grant(rpc: RequestVote, data: Meta): (Vote, Meta) = {
    data.votes = data.votes.vote(rpc.candidateId)
    data.term = Term.max(data.term, rpc.term)
    (GrantVote(data.term), data)
  }

  private def candidateLogTermIsBehind(rpc: RequestVote, data: Meta) =
    data.log.entries.last.term > rpc.lastLogTerm

  private def candidateLogTermIsEqualButHasShorterLog(rpc: RequestVote, data: Meta) =
    (data.log.entries.last.term == rpc.lastLogTerm) &&
      (data.log.entries.length - 1 > rpc.lastLogIndex)

  private def alreadyVoted(rpc: RequestVote, data: Meta): Boolean =
    data.votes.votedFor match {
      case Some(_) if rpc.term == data.term => true
      case Some(_) if rpc.term > data.term => false
      case None => false
    }
}

object Raft {
  def apply(size: Int, ui: ActorRef)(implicit system: ActorSystem): List[NodeId] = {
    val members = for (i <- List.range(0, size))
      yield system.actorOf(Props(classOf[Raft], i, ui), "member" + i)

    members.foreach(m => m ! Init(members))
    members
  }
}
