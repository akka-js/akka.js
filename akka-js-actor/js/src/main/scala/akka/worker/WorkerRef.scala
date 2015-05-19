package akka.worker

import scala.collection.mutable.HashMap
import akka.actor._
import akka.event.{ EventStream, LoggingAdapter }
import akka.dispatch.MessageDispatcher
import akka.dispatch.sysmsg._
import org.scalajs.dom.{ Worker, MessageChannel, MessageEvent }
import java.lang.reflect.Array
import scala.language.implicitConversions
import scala.scalajs.js
import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal
import scala.scalajs.js.annotation.JSExport

trait Transferable extends js.Any

object Transferable {
  import scala.scalajs.js.typedarray.ArrayBuffer
  import org.scalajs.dom.MessagePort
  
  implicit def ArrayBuffer2Transferable(a: ArrayBuffer) = a.asInstanceOf[Transferable]
  implicit def MessagePort2Transferable(p: MessagePort) = p.asInstanceOf[Transferable]
}

package object pm extends js.GlobalScope {
  def postMessage(aMessage: Any, transferList: Seq[Transferable]): Unit = js.native
}

object AkkaWorker {
  import akka.worker.pm
  import js.Dynamic.global
  
  val listener: js.Function1[MessageEvent, _] = { (msg: MessageEvent) =>
    val data = msg.data
  }
  global.onmessage = listener
  
  
  
  val network = new HashMap[String, Worker]
  def apply(stringUrl: String) = network += stringUrl -> new Worker(stringUrl)
  
}

private[akka] trait WorkerRef extends ActorRefScope {
  final def isLocal = false
}

@JSExport
class WorkerActorRefProvider (
                                 _systemName: String,
                                 override val settings: ActorSystem.Settings,
                                 val eventStream: EventStream,
                                 /**
                                  * @note IMPLEMENT IN SCALA.JS
                                  *
                                    val dynamicAccess: DynamicAccess,
                                    override val deployer: Deployer,
                                  */
                                 _deadLetters: Option[ActorPath ⇒ InternalActorRef])
  extends ActorRefProvider {

  // this is the constructor needed for reflectively instantiating the provider
  def this(_systemName: String,
           settings: ActorSystem.Settings,
           eventStream: EventStream
           /** @note IMPLEMENT IN SCALA.JS , dynamicAccess: DynamicAccess */) =
    this(_systemName,
      settings,
      eventStream,
    /**
     * @note IMPLEMENT IN SCALA.JS
     * Deployer not implemented
     * dynamicAccess,
     *  new Deployer(settings, dynamicAccess),
     */
      None)
      
  private val local = new LocalActorRefProvider(_systemName, settings, eventStream)
  
  def log: LoggingAdapter = local.log
  
  
  override def rootPath: ActorPath = local.rootPath
  override def deadLetters: InternalActorRef = local.deadLetters

  // these are only available after init()
  override def rootGuardian: InternalActorRef = local.rootGuardian
  override def guardian: LocalActorRef = local.guardian
  override def systemGuardian: LocalActorRef = local.systemGuardian
  //override def terminationFuture: Future[Terminated] = local.terminationFuture
  override def registerTempActor(actorRef: InternalActorRef, path: ActorPath): Unit = local.registerTempActor(actorRef, path)
  override def unregisterTempActor(path: ActorPath): Unit = local.unregisterTempActor(path)
  override def tempPath(): ActorPath = local.tempPath()
  override def tempContainer: VirtualPathContainer = local.tempContainer
  
  def init(system: ActorSystemImpl): Unit = {
    local.init(system)
  }
  
  def localAddressForRemote(a: Address) = a.copy(protocol = "akka")
  
  @deprecated("use actorSelection instead of actorFor", "2.2")
  def actorFor(path: ActorPath): InternalActorRef = {
    if (hasAddress(path.address)) actorFor(rootGuardian, path.elements)
    else try {
      new WorkerActorRef(localAddressForRemote(path.address), path)
    } catch {
      case NonFatal(e) ⇒
        log.error(e, "Error while looking up address [{}]", path.address)
        new EmptyLocalActorRef(this, path, eventStream)
    }
  }

  @deprecated("use actorSelection instead of actorFor", "2.2")
  def actorFor(ref: InternalActorRef, path: String): InternalActorRef = path match {
    case ActorPathExtractor(address, elems) ⇒
      if (hasAddress(address)) actorFor(rootGuardian, elems)
      else {
        val rootPath = RootActorPath(address) / elems
        try {
          new WorkerActorRef(localAddressForRemote(address), rootPath)
        } catch {
          case NonFatal(e) ⇒
            log.error(e, "Error while looking up address [{}]", rootPath.address)
            new EmptyLocalActorRef(this, rootPath, eventStream)
        }
      }
    case _ ⇒ local.actorFor(ref, path)
  }

  @deprecated("use actorSelection instead of actorFor", "2.2")
  def actorFor(ref: InternalActorRef, path: Iterable[String]): InternalActorRef =
    local.actorFor(ref, path)

  def rootGuardianAt(address: Address): ActorRef =
    if (hasAddress(address)) rootGuardian
    else new WorkerActorRef(localAddressForRemote(address), RootActorPath(address))

  /**
   * INTERNAL API
   * Called in deserialization of incoming remote messages where the correct local address is known.
   */
  private[akka] def resolveActorRefWithLocalAddress(path: String, localAddress: Address): InternalActorRef = {
    path match {
      case ActorPathExtractor(address, elems) ⇒
        if (hasAddress(address)) local.resolveActorRef(rootGuardian, elems)
        else
          new WorkerActorRef(localAddress, RootActorPath(address) / elems)
      case _ ⇒
        log.debug("resolve of unknown path [{}] failed", path)
        deadLetters
    }
  }

  def resolveActorRef(path: String): ActorRef = path match {
    case ActorPathExtractor(address, elems) ⇒
      if (hasAddress(address)) local.resolveActorRef(rootGuardian, elems)
      else {
        val rootPath = RootActorPath(address) / elems
        try {
          new WorkerActorRef(localAddressForRemote(address), rootPath)
        } catch {
          case NonFatal(e) ⇒
            log.error(e, "Error while resolving address [{}]", rootPath.address)
            new EmptyLocalActorRef(this, rootPath, eventStream)
        }
      }
    case _ ⇒
      log.debug("resolve of unknown path [{}] failed", path)
      deadLetters
  }

  def resolveActorRef(path: ActorPath): ActorRef = {
    if (hasAddress(path.address)) local.resolveActorRef(rootGuardian, path.elements)
    else try {
      new WorkerActorRef(localAddressForRemote(path.address), path)
    } catch {
      case NonFatal(e) ⇒
        log.error(e, "Error while resolving address [{}]", path.address)
        new EmptyLocalActorRef(this, path, eventStream)
    }
  }

  def getExternalAddressFor(addr: Address): Option[Address] = {
    addr match {
      case _ if hasAddress(addr)           ⇒ Some(local.rootPath.address)
      case Address(_, _, Some(_), Some(_)) ⇒ try Some(localAddressForRemote(addr)) catch { case NonFatal(_) ⇒ None }
      case _                               ⇒ None
    }
  }

  private def hasAddress(address: Address): Boolean =
    address == local.rootPath.address || address == rootPath.address 

    
  def actorOf(system: ActorSystemImpl,
               props: Props,
               supervisor: InternalActorRef,
               path: ActorPath,
               systemService: Boolean, async: Boolean) = local.actorOf(system, props, supervisor, path, systemService, async)
               
  def getDefaultAddress: akka.actor.Address = local.getDefaultAddress
  
  lazy val terminationPromise: Promise[Unit] = Promise[Unit]()

  def terminationFuture: Future[Unit] = terminationPromise.future
}

private[akka] class WorkerActorRef private[akka] (val localAddressToUse: Address, override val path: ActorPath) extends InternalActorRef with WorkerRef {
  
  def start(): Unit = println("NOT IMPLEMENTED")
  
  def suspend(): Unit = sendSystemMessage(Suspend())

  def resume(causedByFailure: Throwable): Unit = sendSystemMessage(Resume(causedByFailure))

  def stop(): Unit = sendSystemMessage(Terminate())

  def restart(cause: Throwable): Unit = sendSystemMessage(Recreate(cause))
  
  def sendSystemMessage(message: SystemMessage): Unit = println("NOT IMPLEMENTED")
  
  def provider: ActorRefProvider = { println("NOT IMPLEMENTED"); null.asInstanceOf[ActorRefProvider] }
  def getParent: InternalActorRef = { println("NOT IMPLEMENTED"); null.asInstanceOf[InternalActorRef] }
  def getChild(name: Iterator[String]): InternalActorRef = { println("NOT IMPLEMENTED"); null.asInstanceOf[InternalActorRef] }

  def isTerminated: Boolean = { println("NOT IMPLEMENTED"); false }

  override def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit = println("NOT IMPLEMENTED")
  
}