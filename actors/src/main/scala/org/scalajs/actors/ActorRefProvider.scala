/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */

package org.scalajs.actors

import scala.collection.immutable

import sysmsg._
//import routing._
import dispatch._
import event._
import util.{ /*Switch,*/ Helpers }
import util.Collections.EmptyImmutableSeq
import scala.util.{ Success, Failure }
import scala.util.control.NonFatal
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.annotation.implicitNotFound

/**
 * Interface for all ActorRef providers to implement.
 */
trait ActorRefProvider {

  /**
   * Reference to the supervisor of guardian and systemGuardian; this is
   * exposed so that the ActorSystemImpl can use it as lookupRoot, i.e.
   * for anchoring absolute actor look-ups.
   */
  def rootGuardian: InternalActorRef

  /**
   * Reference to the supervisor of guardian and systemGuardian at the specified address;
   * this is exposed so that the ActorRefFactory can use it as lookupRoot, i.e.
   * for anchoring absolute actor selections.
   */
  def rootGuardianAt(address: Address): ActorRef

  /**
   * Reference to the supervisor used for all top-level user actors.
   */
  def guardian: LocalActorRef

  /**
   * Reference to the supervisor used for all top-level system actors.
   */
  def systemGuardian: LocalActorRef

  /**
   * Dead letter destination for this provider.
   */
  def deadLetters: ActorRef

  /**
   * The root path for all actors within this actor system, not including any remote address information.
   */
  def rootPath: ActorPath

  /**
   * The Settings associated with this ActorRefProvider
   */
  def settings: ActorSystem.Settings

  /**
   * Initialization of an ActorRefProvider happens in two steps: first
   * construction of the object with settings, eventStream, etc.
   * and then—when the ActorSystem is constructed—the second phase during
   * which actors may be created (e.g. the guardians).
   */
  def init(system: ActorSystemImpl): Unit

  /**
   * The Deployer associated with this ActorRefProvider
   */
  //def deployer: Deployer

  /**
   * Generates and returns a unique actor path below “/temp”.
   */
  def tempPath(): ActorPath

  /**
   * Returns the actor reference representing the “/temp” path.
   */
  def tempContainer: InternalActorRef

  /**
   * Registers an actorRef at a path returned by tempPath();
   * do NOT pass in any other path.
   */
  def registerTempActor(actorRef: InternalActorRef, path: ActorPath): Unit

  /**
   * Unregister a temporary actor from the “/temp” path
   * (i.e. obtained from tempPath()); do NOT pass in any other path.
   */
  def unregisterTempActor(path: ActorPath): Unit

  /**
   * Actor factory with create-only semantics: will create an actor as
   * described by props with the given supervisor and path (may be different
   * in case of remote supervision). If systemService is true, deployment is
   * bypassed (local-only). If ``Some(deploy)`` is passed in, it should be
   * regarded as taking precedence over the nominally applicable settings,
   * but it should be overridable from external configuration; the lookup of
   * the latter can be suppressed by setting ``lookupDeploy`` to ``false``.
   */
  def actorOf(
    system: ActorSystemImpl,
    props: Props,
    supervisor: InternalActorRef,
    path: ActorPath,
    systemService: Boolean,
    //deploy: Option[Deploy],
    //lookupDeploy: Boolean,
    async: Boolean): InternalActorRef

  /**
   * Create actor reference for a specified path. If no such
   * actor exists, it will be (equivalent to) a dead letter reference.
   */
  //def resolveActorRef(path: String): ActorRef

  /**
   * Create actor reference for a specified path. If no such
   * actor exists, it will be (equivalent to) a dead letter reference.
   */
  def resolveActorRef(path: ActorPath): ActorRef

  /**
   * This Future is completed upon termination of this ActorRefProvider, which
   * is usually initiated by stopping the guardian via ActorSystem.stop().
   */
  def terminationFuture: Future[Unit]

  /**
   * Obtain the address which is to be used within sender references when
   * sending to the given other address or none if the other address cannot be
   * reached from this system (i.e. no means of communication known; no
   * attempt is made to verify actual reachability).
   */
  def getExternalAddressFor(addr: Address): Option[Address]

  /**
   * Obtain the external address of the default transport.
   */
  def getDefaultAddress: Address
}

/**
 * INTERNAL API
 */
private[actors] object SystemGuardian {
  /**
   * For the purpose of orderly shutdown it's possible
   * to register interest in the termination of systemGuardian
   * and receive a notification [[akka.actor.Guardian.TerminationHook]]
   * before systemGuardian is stopped. The registered hook is supposed
   * to reply with [[akka.actor.Guardian.TerminationHookDone]] and the
   * systemGuardian will not stop until all registered hooks have replied.
   */
  case object RegisterTerminationHook
  case object TerminationHook
  case object TerminationHookDone
}

private[actors] object LocalActorRefProvider {

  /*
   * Root and user guardian
   */
  private class Guardian(override val supervisorStrategy: SupervisorStrategy)
      extends Actor {

    def receive = {
      case Terminated(_)    => context.stop(self)
      case StopChild(child) => context.stop(child)
      case m                => context.system.deadLetters forward DeadLetter(m, sender, self)
    }

    // guardian MUST NOT lose its children during restart
    override def preRestart(cause: Throwable, msg: Option[Any]) {}
  }

  /**
   * System guardian
   */
  private class SystemGuardian(
      override val supervisorStrategy: SupervisorStrategy,
      val guardian: ActorRef)
      extends Actor {
    import SystemGuardian._

    var terminationHooks = Set.empty[ActorRef]

    def receive = {
      case Terminated(`guardian`) =>
        // time for the systemGuardian to stop, but first notify all the
        // termination hooks, they will reply with TerminationHookDone
        // and when all are done the systemGuardian is stopped
        context.become(terminating)
        terminationHooks foreach { _ ! TerminationHook }
        stopWhenAllTerminationHooksDone()
      case Terminated(a) =>
        // a registered, and watched termination hook terminated before
        // termination process of guardian has started
        terminationHooks -= a
      case StopChild(child) => context.stop(child)
      case RegisterTerminationHook if sender != context.system.deadLetters =>
        terminationHooks += sender
        context watch sender
      case m => context.system.deadLetters forward DeadLetter(m, sender, self)
    }

    def terminating: Receive = {
      case Terminated(a)       => stopWhenAllTerminationHooksDone(a)
      case TerminationHookDone => stopWhenAllTerminationHooksDone(sender)
      case m                   => context.system.deadLetters forward DeadLetter(m, sender, self)
    }

    def stopWhenAllTerminationHooksDone(remove: ActorRef): Unit = {
      terminationHooks -= remove
      stopWhenAllTerminationHooksDone()
    }

    def stopWhenAllTerminationHooksDone(): Unit =
      if (terminationHooks.isEmpty) {
        //context.system.eventStream.stopDefaultLoggers(context.system)
        context.stop(self)
      }

    // guardian MUST NOT lose its children during restart
    override def preRestart(cause: Throwable, msg: Option[Any]) {}
  }

}

/**
 * Local ActorRef provider.
 *
 * INTERNAL API!
 *
 * Depending on this class is not supported, only the [[ActorRefProvider]] interface is supported.
 */
private[actors] class LocalActorRefProvider private[actors] (
  _systemName: String,
  override val settings: ActorSystem.Settings,
  val eventStream: EventStream,
  //val dynamicAccess: DynamicAccess,
  //override val deployer: Deployer,
  _deadLetters: Option[ActorPath => InternalActorRef])
  extends ActorRefProvider {

  // this is the constructor needed for reflectively instantiating the provider
  def this(_systemName: String,
           settings: ActorSystem.Settings,
           eventStream: EventStream/*,
           dynamicAccess: DynamicAccess*/) =
    this(_systemName,
      settings,
      eventStream,
      //dynamicAccess,
      //new Deployer(settings, dynamicAccess),
      None)

  override val rootPath: ActorPath = RootActorPath(Address("akka", _systemName))

  //private[actors] val log: LoggingAdapter =
  //  Logging(eventStream, "LocalActorRefProvider(" + rootPath.address + ")")
  protected object log {
    def error(msg: String): Unit = Console.err.println(msg)
    def error(ex: Throwable, msg: String): Unit = error(s"$ex -- $msg")
    def debug(msg: String): Unit = Console.err.println(msg)
    def debug(ex: Throwable, msg: String): Unit = debug(s"$ex -- $msg")
  }

  override val deadLetters: InternalActorRef =
    _deadLetters.getOrElse((p: ActorPath) =>
      new DeadLetterActorRef(this, p, eventStream)).apply(rootPath / "deadLetters")

  /*
   * generate name for temporary actor refs
   */
  private var tempNumber = 0L

  private def tempName() = Helpers.base64({
    val n = tempNumber
    tempNumber += 1
    n
  })

  private val tempNode = rootPath / "temp"

  override def tempPath(): ActorPath = tempNode / tempName()

  /**
   * Top-level anchor for the supervision hierarchy of this actor system. Will
   * receive only Supervise/ChildTerminated system messages or Failure message.
   */
  private[actors] val theOneWhoWalksTheBubblesOfSpaceTime: InternalActorRef = new MinimalActorRef {
    private[this] var stopped: Boolean = false

    var causeOfTermination: Option[Throwable] = None

    val path = (rootPath / "bubble-walker") withUid 1

    def provider: ActorRefProvider = LocalActorRefProvider.this

    override def stop(): Unit = {
      if (!stopped) {
        stopped = true
        terminationPromise.complete(
            causeOfTermination.map(Failure(_)).getOrElse(Success(())))
      }
    }

    override def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit = {
      if (!stopped) {
        message match {
          case null => throw new InvalidMessageException("Message is null")
          case _    =>
            log.error(s"$this received unexpected message [$message]")
        }
      }
    }

    override def sendSystemMessage(message: SystemMessage): Unit = {
      if (!stopped) {
        message match {
          case Failed(child, ex, _) =>
            log.error(ex, s"guardian $child failed, shutting down!")
            causeOfTermination = Some(ex)
            child.asInstanceOf[InternalActorRef].stop()
          case Supervise(_, _) =>
            // TODO register child in some map to keep track of it and enable shutdown after all dead
          case _: DeathWatchNotification =>
            stop()
          case _ =>
            log.error(s"$this received unexpected system message [$message]")
        }
      }
    }
  }

  /*
   * The problem is that ActorRefs need a reference to the ActorSystem to
   * provide their service. Hence they cannot be created while the
   * constructors of ActorSystem and ActorRefProvider are still running.
   * The solution is to split out that last part into an init() method,
   * but it also requires these references to be @volatile and lazy.
   */
  private var system: ActorSystemImpl = _

  lazy val terminationPromise: Promise[Unit] = Promise[Unit]()

  def terminationFuture: Future[Unit] = terminationPromise.future

  private var extraNames: Map[String, InternalActorRef] = Map()

  /**
   * Higher-level providers (or extensions) might want to register new synthetic
   * top-level paths for doing special stuff. This is the way to do just that.
   * Just be careful to complete all this before ActorSystem.start() finishes,
   * or before you start your own auto-spawned actors.
   */
  def registerExtraNames(_extras: Map[String, InternalActorRef]): Unit =
    extraNames ++= _extras

  //private def guardianSupervisorStrategyConfigurator =
  //  dynamicAccess.createInstanceFor[SupervisorStrategyConfigurator](settings.SupervisorStrategyClass, EmptyImmutableSeq).get

  /**
   * Overridable supervision strategy to be used by the “/user” guardian.
   */
  protected def rootGuardianStrategy: SupervisorStrategy = OneForOneStrategy() {
    case ex =>
      log.error(ex, "guardian failed, shutting down system")
      SupervisorStrategy.Stop
  }

  /**
   * Overridable supervision strategy to be used by the “/user” guardian.
   */
  protected def guardianStrategy: SupervisorStrategy =
    SupervisorStrategy.defaultStrategy
    //guardianSupervisorStrategyConfigurator.create()

  /**
   * Overridable supervision strategy to be used by the “/system” guardian.
   */
  protected def systemGuardianStrategy: SupervisorStrategy =
    SupervisorStrategy.defaultStrategy

  val mailboxes: Mailboxes = new Mailboxes(deadLetters)
  private lazy val defaultDispatcher: MessageDispatcher =
    new MessageDispatcher(mailboxes)
    //system.dispatchers.defaultGlobalDispatcher

  //private lazy val defaultMailbox = system.mailboxes.lookup(Mailboxes.DefaultMailboxId)

  override lazy val rootGuardian: LocalActorRef =
    new LocalActorRef(
        system,
        rootPath,
        theOneWhoWalksTheBubblesOfSpaceTime,
        Props(new LocalActorRefProvider.Guardian(rootGuardianStrategy)),
        defaultDispatcher) {
      override def getParent: InternalActorRef = this
      override def getSingleChild(name: String): InternalActorRef = name match {
        case "temp"        => tempContainer
        case "deadLetters" => deadLetters
        case other         =>
          extraNames.get(other).getOrElse(super.getSingleChild(other))
      }
    }

  override def rootGuardianAt(address: Address): ActorRef =
    if (address == rootPath.address) rootGuardian
    else deadLetters

  override lazy val guardian: LocalActorRef = {
    val cell = rootGuardian.actorCell
    cell.checkChildNameAvailable("user")
    val ref = new LocalActorRef(system, (rootPath / "user") withUid 2, rootGuardian,
        Props(new LocalActorRefProvider.Guardian(guardianStrategy)),
        defaultDispatcher)
    cell.initChild(ref)
    ref.start()
    ref
  }

  override lazy val systemGuardian: LocalActorRef = {
    val cell = rootGuardian.actorCell
    cell.checkChildNameAvailable("system")
    val ref = new LocalActorRef(system, (rootPath / "system") withUid 3, rootGuardian,
        Props(new LocalActorRefProvider.SystemGuardian(systemGuardianStrategy, guardian)),
        defaultDispatcher)
    cell.initChild(ref)
    ref.start()
    ref
  }

  lazy val tempContainer =
    new VirtualPathContainer(system.provider, tempNode, rootGuardian/*, log*/)

  def registerTempActor(actorRef: InternalActorRef, path: ActorPath): Unit = {
    assert(path.parent eq tempNode, "cannot registerTempActor() with anything not obtained from tempPath()")
    tempContainer.addChild(path.name, actorRef)
  }

  def unregisterTempActor(path: ActorPath): Unit = {
    assert(path.parent eq tempNode, "cannot unregisterTempActor() with anything not obtained from tempPath()")
    tempContainer.removeChild(path.name)
  }

  def init(_system: ActorSystemImpl) {
    system = _system
    rootGuardian.start()
    // chain death watchers so that killing guardian stops the application
    systemGuardian.sendSystemMessage(Watch(guardian, systemGuardian))
    rootGuardian.sendSystemMessage(Watch(systemGuardian, rootGuardian))
    //eventStream.startDefaultLoggers(_system)
  }

  /*def resolveActorRef(path: String): ActorRef = path match {
    case ActorPathExtractor(address, elems) if address == rootPath.address =>
      resolveActorRef(rootGuardian, elems)
    case _ =>
      log.debug(s"resolve of unknown path [$path] failed")
      deadLetters
  }*/

  def resolveActorRef(path: ActorPath): ActorRef = {
    if (path.root == rootPath) resolveActorRef(rootGuardian, path.elements)
    else {
      log.debug(s"resolve of foreign ActorPath [$path] failed")
      deadLetters
    }
  }

  /**
   * INTERNAL API
   */
  private[actors] def resolveActorRef(ref: InternalActorRef,
      pathElements: Iterable[String]): InternalActorRef =
    if (pathElements.isEmpty) {
      log.debug("resolve of empty path sequence fails (per definition)")
      deadLetters
    } else ref.getChild(pathElements.iterator) match {
      case Nobody =>
        log.debug(s"resolve of path sequence [/${pathElements.mkString("/")}] failed")
        new EmptyLocalActorRef(system.provider, ref.path / pathElements, eventStream)
      case x => x
    }

  def actorOf(system: ActorSystemImpl, props: Props, supervisor: InternalActorRef, path: ActorPath,
              systemService: Boolean, /*deploy: Option[Deploy], lookupDeploy: Boolean,*/ async: Boolean): InternalActorRef = {
    new LocalActorRef(system, path, supervisor, props, system.dispatcher)
  }

  def getExternalAddressFor(addr: Address): Option[Address] =
    if (addr == rootPath.address) Some(addr) else None

  def getDefaultAddress: Address = rootPath.address
}
