package akka.actor

import akka.event.EventStream
import akka.dispatch.{ UnboundedMessageQueueSemantics, RequiresMessageQueue, Mailboxes }
import scala.util.control.NonFatal
import akka.routing.{ NoRouter, JSRouterActorCreator, RoutedActorRef }
import akka.ConfigurationException


private[akka] object JSLocalActorRefProvider {

  /*
   * Root and user guardian
   */
  private class Guardian(override val supervisorStrategy: SupervisorStrategy) extends Actor
    with RequiresMessageQueue[UnboundedMessageQueueSemantics] {

    def receive = {
      case Terminated(_)    ⇒ context.stop(self)
      case StopChild(child) ⇒ context.stop(child)
    }

    // guardian MUST NOT lose its children during restart
    override def preRestart(cause: Throwable, msg: Option[Any]) {}
  }

  /**
   * System guardian
   */
  private class SystemGuardian(override val supervisorStrategy: SupervisorStrategy, val guardian: ActorRef)
    extends Actor with RequiresMessageQueue[UnboundedMessageQueueSemantics] {
    import SystemGuardian._

    var terminationHooks = Set.empty[ActorRef]

    def receive = {
      case Terminated(`guardian`) ⇒
        // time for the systemGuardian to stop, but first notify all the
        // termination hooks, they will reply with TerminationHookDone
        // and when all are done the systemGuardian is stopped
        context.become(terminating)
        terminationHooks foreach { _ ! TerminationHook }
        stopWhenAllTerminationHooksDone()
      case Terminated(a) ⇒
        // a registered, and watched termination hook terminated before
        // termination process of guardian has started
        terminationHooks -= a
      case StopChild(child) ⇒ context.stop(child)
      case RegisterTerminationHook if sender() != context.system.deadLetters ⇒
        terminationHooks += sender()
        context watch sender()
    }

    def terminating: Receive = {
      case Terminated(a)       ⇒ stopWhenAllTerminationHooksDone(a)
      case TerminationHookDone ⇒ stopWhenAllTerminationHooksDone(sender())
    }

    def stopWhenAllTerminationHooksDone(remove: ActorRef): Unit = {
      terminationHooks -= remove
      stopWhenAllTerminationHooksDone()
    }

    def stopWhenAllTerminationHooksDone(): Unit =
      if (terminationHooks.isEmpty) {
        context.system.eventStream.stopDefaultLoggers(context.system)
        context.stop(self)
      }

    // guardian MUST NOT lose its children during restart
    override def preRestart(cause: Throwable, msg: Option[Any]) {}
  }

}

@scala.scalajs.js.annotation.JSExport
class JSLocalActorRefProvider (
  _systemName: String,
  override val settings: ActorSystem.Settings,
  val _eventStream: EventStream,
  val _dynamicAccess: DynamicAccess/*,
  override val deployer: Deployer,
  _deadLetters: Option[ActorPath ⇒ InternalActorRef]*/) extends LocalActorRefProvider(
  _systemName,
  settings,
  _eventStream,
  _dynamicAccess,
  null,
  None) {

  private var system: ActorSystemImpl = _

  private lazy val defaultDispatcher = system.dispatchers.defaultGlobalDispatcher

  private lazy val defaultMailbox = system.mailboxes.lookup(Mailboxes.DefaultMailboxId)

  @volatile
  private var extraNames: Map[String, InternalActorRef] = Map()

  override def registerExtraNames(_extras: Map[String, InternalActorRef]): Unit = {
    extraNames ++= _extras
    super.registerExtraNames(_extras)
  }

  override def init(_system: ActorSystemImpl) {
    system = _system
    super.init(_system)
  }

  //Guardians are super private and rigth now they cannot be exported...
  override lazy val rootGuardian: LocalActorRef =
    new LocalActorRef(
      system,
      Props(
        new JSLocalActorRefProvider.Guardian(rootGuardianStrategy)),
      defaultDispatcher,
      defaultMailbox,
      theOneWhoWalksTheBubblesOfSpaceTime,
      rootPath) {
      override def getParent: InternalActorRef = this
      override def getSingleChild(name: String): InternalActorRef = name match {
        case "temp"        ⇒ tempContainer
        case "deadLetters" ⇒ deadLetters
        case other         ⇒ extraNames.get(other).getOrElse(super.getSingleChild(other))
      }
    }

  override lazy val guardian: LocalActorRef = {
    val cell = rootGuardian.underlying
    cell.reserveChild("user")
    val ref = new LocalActorRef(system, system.guardianProps.getOrElse(Props(
        new JSLocalActorRefProvider.Guardian(SupervisorStrategy.defaultStrategy))),
      defaultDispatcher, defaultMailbox, rootGuardian, rootPath / "user")
    cell.initChild(ref)
    ref.start()
    ref
  }

  override lazy val systemGuardian: LocalActorRef = {
    val cell = rootGuardian.underlying
    cell.reserveChild("system")
    val ref = new LocalActorRef(
      system, Props(
        new JSLocalActorRefProvider.SystemGuardian(systemGuardianStrategy, guardian)),
      defaultDispatcher, defaultMailbox, rootGuardian, rootPath / "system")
    cell.initChild(ref)
    ref.start()
    ref
  }


  override def actorOf(system: ActorSystemImpl, props: Props, supervisor: InternalActorRef, path: ActorPath,
              systemService: Boolean, deploy: Option[Deploy], lookupDeploy: Boolean, async: Boolean): InternalActorRef = {
 props.deploy.routerConfig match {
   case NoRouter ⇒
     val props2 = props

     try {
       val dispatcher = system.dispatchers.lookup(akka.dispatch.Dispatchers.DefaultDispatcherId)
       val mailboxType = system.mailboxes.getMailboxType(props2, dispatcher.configurator.config)

       if (async) new RepointableActorRef(system, props2, dispatcher, mailboxType, supervisor, path).initialize(async)
       else new LocalActorRef(system, props2, dispatcher, mailboxType, supervisor, path)
     } catch {
       case NonFatal(e) ⇒
         throw new ConfigurationException(
           s"configuration problem while creating [$path] with dispatcher [${props2.dispatcher}] and mailbox [${props2.mailbox}]", e)
     }

   case router ⇒

     val lookup = None
     val fromProps = Iterator(props.deploy.copy(routerConfig = props.deploy.routerConfig withFallback router))
     val d = fromProps ++ deploy.iterator ++ lookup.iterator reduce ((a, b) ⇒ b withFallback a)
     val p = props.withRouter(d.routerConfig)

     val routerProps = Props(p.deploy.copy(dispatcher = p.routerConfig.routerDispatcher),
       classOf[JSRouterActorCreator], Vector(p.routerConfig))
     val routeeProps = p.withRouter(NoRouter)

     try {
       val routerDispatcher = system.dispatchers.lookup(p.routerConfig.routerDispatcher)
       val routerMailbox = system.mailboxes.getMailboxType(routerProps, routerDispatcher.configurator.config)

       // routers use context.actorOf() to create the routees, which does not allow us to pass
       // these through, but obtain them here for early verification
       val routeeDispatcher = system.dispatchers.lookup(p.dispatcher)
       val routeeMailbox = system.mailboxes.getMailboxType(routeeProps, routeeDispatcher.configurator.config)

       new RoutedActorRef(system, routerProps, routerDispatcher, routerMailbox, routeeProps, supervisor, path).initialize(async)
     } catch {
       case NonFatal(e) ⇒ throw new ConfigurationException(
         s"configuration problem while creating [$path] with router dispatcher [${routerProps.dispatcher}] and mailbox [${routerProps.mailbox}] " +
           s"and routee dispatcher [${routeeProps.dispatcher}] and mailbox [${routeeProps.mailbox}]", e)
     }
   }
 }

}
