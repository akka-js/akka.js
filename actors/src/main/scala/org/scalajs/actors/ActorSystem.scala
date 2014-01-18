package org.scalajs.actors

import scala.concurrent.Future

import dispatch._
import event._

object ActorSystem {

  /**
   * Creates a new ActorSystem with the name "default",
   * obtains the current ClassLoader by first inspecting the current threads' getContextClassLoader,
   * then tries to walk the stack to find the callers class loader, then falls back to the ClassLoader
   * associated with the ActorSystem class.
   * Then it loads the default reference configuration using the ClassLoader.
   */
  def apply(): ActorSystem = apply("default")

  /**
   * Creates a new ActorSystem with the specified name,
   * obtains the current ClassLoader by first inspecting the current threads' getContextClassLoader,
   * then tries to walk the stack to find the callers class loader, then falls back to the ClassLoader
   * associated with the ActorSystem class.
   * Then it loads the default reference configuration using the ClassLoader.
   */
  def apply(name: String): ActorSystem = {
    new ActorSystemImpl(new Settings(name)).start()
  }

  /**
   * Settings are the overall ActorSystem Settings which also provides a convenient access to the Config object.
   *
   * For more detailed information about the different possible configuration options, look in the Akka Documentation under "Configuration"
   *
   * @see <a href="http://typesafehub.github.com/config/v0.4.1/" target="_blank">The Typesafe Config Library API Documentation</a>
   */
  class Settings(final val name: String) {

    final val LogDeadLetters: Int = 0
    final val LogDeadLettersDuringShutdown: Boolean = false

    final val AddLoggingReceive: Boolean = true
    final val DebugAutoReceive: Boolean = false
    final val DebugLifecycle: Boolean = true
    final val DebugEventStream: Boolean = false
    final val DebugUnhandledMessage: Boolean = false

    override def toString: String = s"Settings($name)"

  }
}

abstract class ActorSystem(val settings: ActorSystem.Settings) extends ActorRefFactory {
  val name: String = settings.name

  def scheduler: Scheduler
  def eventStream: EventStream
  def provider: ActorRefProvider

  def deadLetters: ActorRef

  def shutdown(): Unit

  def sendToPath(path: ActorPath, message: Any)(
      implicit sender: ActorRef = Actor.noSender): Unit
}

class ActorSystemImpl(_settings: ActorSystem.Settings)
    extends ActorSystem(_settings) with webworkers.WebWorkersActorSystem {

  protected def systemImpl = this

  def actorOf(props: Props): ActorRef =
    guardian.actorCell.actorOf(props)
  def actorOf(props: Props, name: String): ActorRef =
    guardian.actorCell.actorOf(props, name)

  def stop(actor: ActorRef): Unit = {
    val path = actor.path
    val guard = guardian.path
    val sys = systemGuardian.path
    path.parent match {
      case `guard` => guardian ! StopChild(actor)
      case `sys`   => systemGuardian ! StopChild(actor)
      case _       => actor.asInstanceOf[InternalActorRef].stop()
    }
  }

  val scheduler: Scheduler = new EventLoopScheduler
  val eventStream: EventStream = new EventStream
  val provider: ActorRefProvider = new LocalActorRefProvider(
      name, settings, eventStream)

  def deadLetters: ActorRef = provider.deadLetters

  val mailboxes: Mailboxes = new Mailboxes(deadLetters)
  val dispatcher: MessageDispatcher = new MessageDispatcher(mailboxes)

  def terminationFuture: Future[Unit] = provider.terminationFuture
  def lookupRoot: InternalActorRef = provider.rootGuardian
  def guardian: LocalActorRef = provider.guardian
  def systemGuardian: LocalActorRef = provider.systemGuardian

  def /(actorName: String): ActorPath = guardian.path / actorName
  def /(path: Iterable[String]): ActorPath = guardian.path / path

  private lazy val _start: this.type = {
    // the provider is expected to start default loggers, LocalActorRefProvider does this
    provider.init(this)
    //if (settings.LogDeadLetters > 0)
    //  logDeadLetterListener = Some(systemActorOf(Props[DeadLetterListener], "deadLetterListener"))
    //registerOnTermination(stopScheduler())
    //loadExtensions()
    //if (LogConfigOnStart) logConfiguration()
    this
  }

  def start(): this.type = _start

  def shutdown(): Unit = {
    guardian.stop()
  }

  override def sendToPath(path: ActorPath, message: Any)(
      implicit sender: ActorRef): Unit = {
    // FIXME The existence of this method is a hack! Need to find a solution.
    new webworkers.WorkerActorRef(this, path) ! message
  }

  def resolveLocalActorPath(path: ActorPath): Option[ActorRef] = {
    println(path.elements)
    //Some(provider.resolveActorRef(provider.rootPath / path.elements))
    val result = path.elements.tail.foldLeft(provider.guardian) { (parent, childName) =>
      parent.actorCell.child(childName) match {
        case Some(child: LocalActorRef) => child
        case x =>
          println(s"$parent of name ${parent.path}.child($childName) = $x")
          return None
      }
    }
    Some(result)
  }
}
