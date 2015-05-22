package akka.worker

import scala.collection.mutable.{ MutableList, HashMap }
import akka.actor._
import akka.event.{ EventStream, LoggingAdapter }
import akka.dispatch.MessageDispatcher
import akka.dispatch.sysmsg._
import org.scalajs.dom.{ Worker, MessageEvent }
import java.lang.reflect.Array
import scala.language.implicitConversions
import scala.scalajs.js
import scala.concurrent.{ Future, Promise }
import scala.util.control.NonFatal
import scala.scalajs.js.annotation.JSExport
//import be.doeraene.spickling._
//import be.doeraene.spickling.jsany._
import prickle._
import org.scalajs.dom.MessagePort
import js.JSConverters._

trait Transferable extends js.Any

object Transferable {
  import scala.scalajs.js.typedarray.ArrayBuffer

  
  implicit def ArrayBuffer2Transferable(a: ArrayBuffer) = a.asInstanceOf[Transferable]
  implicit def MessagePort2Transferable(p: MessagePort) = p.asInstanceOf[Transferable]
}

class MessageChannel extends js.Object {
  def port2: MessagePort = js.native

  def port1: MessagePort = js.native
}

package object pm extends js.GlobalScope {
  def postMessage(aMessage: Any, transferList: Seq[Transferable]): Unit = js.native
  def postMessage(aMessage: Any): Unit = js.native
}

case class SendMessage(message: AnyRef, receiver: String, sender: Option[String])

sealed trait WorkerMessage
case class ChannelFor(port: Int) extends WorkerMessage
case class OwnPort(port: Int) extends WorkerMessage

object AkkaWorkerMaster {
  import AkkaWorker.workerMessagePickler
  
  private val workers = new MutableList[Worker]()
  
  def getPortFor(w: Worker): Int = workers.indexOf(w) + 1

  def apply(stringUrl: String): Int = {
    val w = new Worker(stringUrl)
    
    workers += w
    val port = getPortFor(w)
    
    w.postMessage(Pickle.intoString[WorkerMessage](OwnPort(port)))
    
    // channel to main
    val channel = new MessageChannel
    w.postMessage(Pickle.intoString[WorkerMessage](ChannelFor(0)), Seq(channel.port2).toJSArray)
    AkkaWorkerSlave.add(port, channel.port1)
    
    workers filter (_ != w) foreach { otherW =>
      val otherPort = getPortFor(otherW)
            
      val channel = new MessageChannel
      
      w.postMessage(Pickle.intoString[WorkerMessage](ChannelFor(otherPort)), Seq(channel.port1).toJSArray)
            
      otherW.postMessage(Pickle.intoString[WorkerMessage](ChannelFor(port)), Seq(channel.port2).toJSArray)
    }
    
    port
  }
  
}

object AkkaWorker {
  def isWorker: Boolean = js.isUndefined(js.Dynamic.global.document) 
  implicit val workerMessagePickler: PicklerPair[WorkerMessage] = 
    CompositePickler[WorkerMessage]
      .concreteType[ChannelFor]
      .concreteType[OwnPort]
  
  case class Boxed[T](value: T)
  
  implicit val anyPickler: PicklerPair[AnyRef] = 
      CompositePickler[AnyRef]
        .concreteType[String]
        .concreteType[Boxed[Boolean]] 
        .concreteType[Boxed[Byte]]
        .concreteType[Boxed[Char]]
        .concreteType[Boxed[Short]]
        .concreteType[Boxed[Int]]
        .concreteType[Boxed[Long]]
        .concreteType[Boxed[Float]]
        .concreteType[Boxed[Double]]
}

object AkkaWorkerSlave {
  import AkkaWorker.{ workerMessagePickler, anyPickler }
  
  import akka.worker.pm
  import js.Dynamic.global
  
  private val systems = new HashMap[String, ActorSystem]()
  private val network = new HashMap[Int, MessagePort]()
  private var ownPort = 0
  
  def getOwnPort = ownPort
  
  def addSystem(system: ActorSystem) = systems += system.name -> system
  
  val workerOnMessage: js.Function1[js.Any, _] = { (message: js.Any) =>
    val event = message.asInstanceOf[MessageEvent]
    
    val data = Unpickle[WorkerMessage].fromString(event.data.asInstanceOf[String])
    
    data.get match {
      case ChannelFor(w) =>
        val port = event.ports.asInstanceOf[js.Array[MessagePort]](0)
        add(w, port)
      case OwnPort(p) =>
        ownPort = p
    }
    
  }

  def get(i: Int): MessagePort = network(i)
  def add(i: Int, port: MessagePort) = {
    network += i -> port
    
    port.onmessage = { (message: js.Any) =>
      val sendMessage = Unpickle[SendMessage].fromString(message.asInstanceOf[MessageEvent].data.asInstanceOf[String]).get
      
      val systemName = ActorPath.fromString(sendMessage.receiver).address.system
      
      val m: Any = sendMessage.message match {
        case AkkaWorker.Boxed(t) => t
        case e => e
      }
      
      sendMessage match {
        case SendMessage(_, r, None) => systems(systemName).actorFor(r) ! m
        case SendMessage(_, r, Some(s)) => systems(systemName).actorFor(r).!(m)(systems(systemName).actorFor(s))
      }
    }
  }
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
    import scala.scalajs.js.Dynamic.global
    import AkkaWorker.workerMessagePickler
    
    local.init(system)
    AkkaWorkerSlave.addSystem(system)
    
    if(AkkaWorker.isWorker) {
      if(global.onmessage == null) global.onmessage = AkkaWorkerSlave.workerOnMessage

      //pm.postMessage(Pickle.intoString[WorkerMessage](HookActorSystem))
    }
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

  private def remoteLocal(path: ActorPath, protocol: String, host: Option[String], port: Option[Int]) = {
    val address = path.address.copy(protocol = protocol, host = host, port = port)
    
    (RootActorPath(address) / path.elements).toString
  }
  
  override def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit = {
    import java.{ lang => jl }
    val port = AkkaWorkerSlave.get(this.path.address.port.get)
    
    val localPath: String = (RootActorPath(this.path.address.copy(protocol = "akka", host = None, port = None)) / this.path.elements).toString()
    
    val refdMessage: AnyRef =
      message match {
        case b: jl.Boolean => AkkaWorker.Boxed(Boolean.unbox(b))
        case b: jl.Byte => AkkaWorker.Boxed(Byte.unbox(b))
        case c: jl.Character => AkkaWorker.Boxed(Char.unbox(c))
        case s: jl.Short => AkkaWorker.Boxed(Char.unbox(s))
        case i: jl.Integer => AkkaWorker.Boxed(Int.unbox(i))
        case l: jl.Long => AkkaWorker.Boxed(Long.unbox(l))
        case f: jl.Float => AkkaWorker.Boxed(Float.unbox(f))
        case d: jl.Double => AkkaWorker.Boxed(Double.unbox(d))
        case a: AnyRef => a
        case any => AkkaWorker.Boxed(any)
      }/* match {
        case a: AnyRef => a
        case b: Boolean => AkkaWorker.Boxed(b)
        case b: Byte => AkkaWorker.Boxed(b)
        case c: Char => AkkaWorker.Boxed(c)
        case s: Short => AkkaWorker.Boxed(s)
        case i: Int => AkkaWorker.Boxed(i)
        case l: Long => AkkaWorker.Boxed(l)
        case f: Float => AkkaWorker.Boxed(f)
        case d: Double => AkkaWorker.Boxed(d)
      })*/
    
    import AkkaWorker.anyPickler
    
    val pickle = Pickle.intoString(
        SendMessage(
          refdMessage, 
          localPath, 
          if(sender == Actor.noSender) None else Some(remoteLocal(sender.path, "akka.cm", Some("127.0.0.1"), Some(AkkaWorkerSlave.getOwnPort)))  
        ))
    
    port.postMessage(pickle.asInstanceOf[js.Any])
  }
  
}
