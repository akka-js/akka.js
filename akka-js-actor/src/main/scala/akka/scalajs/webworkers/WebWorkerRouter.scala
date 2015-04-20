package akka.scalajs.webworkers

import scala.scalajs.js
import js.Dynamic.global
import org.scalajs.dom.Worker
import scalajs.js.timers.setTimeout
import scala.scalajs.js.DynamicImplicits._

import akka.actor._
import akka.util.JSMap

trait WorkerConnection extends org.scalajs.dom.EventTarget {
  def postMessage(message: js.Any): Unit = js.native

  var onmessage: js.Function1[MessageEvent, _] = js.native
}
object ParentWorkerConnection extends WorkerConnection with js.GlobalScope

trait MessageEvent extends js.Object {
  val data: js.Dynamic = js.native // XXX: FIX! should inherit from MessageEvent, router has to be rewritten!
}

/** Router of the current web worker.
 *  It provides a means to send messages to any actor system on any worker in
 *  the same hierarchy.
 *  Typically, the root of the hierarchy is the script on the page. The tree
 *  follows the physical construction of workers.
 *  Since there is exactly one router per worker, this is a top-level object.
 *
 *  As a user, you need to call either initializeAsRoot() or setupAsChild().
 *  You can register blocks of code to be executed as soon as the router is
 *  initialized with onInitialized().
 */
object WebWorkerRouter {
  private var myAddress: String = ""
  private var nextChildAddress = 1
  private val children = JSMap.empty[Worker]
  private val systems = JSMap.empty[ActorSystemImpl]
  private var onInitializedEvents: List[() => _] = Nil

  def isInitialized: Boolean = myAddress != ""

  /** Returns the address of this router.
   *  Requires this router to be initialized.
   */
  def address: String = {
    assertInitialized()
    myAddress
  }

  /** Returns true iff this router is the root of its hierarchy.
   *  Requires this router to be initialized.
   */
  def isRoot: Boolean = {
    assertInitialized()
    myAddress == "/"
  }

  /** Returns the address of the parent of this router.
   *  Requires this router to be initialized and not to be the root.
   */
  def parentAddress: String = {
    assert(!isRoot, "Root router has no parent")
    val pos = myAddress.lastIndexOf('/', myAddress.length-2)
    myAddress.substring(0, pos+1)
  }

  private def assertInitialized(): Unit = {
    assert(isInitialized,
        "Operation cannot be executed when router is not initialized")
  }

  /** Initialize this router as the root of a hierarchy.
   *  Pending onInitialized blocks are executed immediately.
   */
  def initializeAsRoot(): Unit = {
    initializeWithAddress("/")
  }

  /** Setup this router as a child.
   *  The router will listen from an initialization message coming from the
   *  parent worker.
   */
  def setupAsChild(): Unit = {
    val cruiseListener: js.Function1[js.Object, _] = { (event: js.Object) =>
      val data = event.asInstanceOf[MessageEvent].data
      global.console.log("Child WebWorkerRouter receives: " + js.JSON.stringify(data))
      if (!(!data.isWebWorkerRouterPostMessage)) {
        forwardMessage(data)
      }
    }
    var initializeListener: js.Function1[js.Object, _] = null
    initializeListener = { (event: js.Object) =>
      val data = event.asInstanceOf[MessageEvent].data
      global.console.log("Child WebWorkerRouter receives: " + js.JSON.stringify(data))
      if (!(!data.isWebWorkerRouterInitialize)) {
        ParentWorkerConnection.removeEventListener(
            "message", initializeListener, useCapture = false)
        ParentWorkerConnection.addEventListener(
            "message", cruiseListener, useCapture = false)

        val address = data.address.asInstanceOf[String]
        initializeWithAddress(address)
      } else if (!(!data.isWebWorkerRouterPostMessage)) {
        onInitialized {
          forwardMessage(data)
        }
      }
    }
    ParentWorkerConnection.addEventListener(
        "message", initializeListener, useCapture = false)
  }

  /** Execute a block of code when the router has been initialized.
   *  If the router is already initialized, the block of code is executed
   *  immediately.
   */
  def onInitialized(body: => Unit): Unit = {
    if (isInitialized) {
      body
    } else {
      onInitializedEvents ::= (() => body)
    }
  }

  /** Posts a message to the given system at the given address.
   *  If the router is not yet initialized, the message will be queued up and
   *  sent when initialization is done.
   */
  def postMessageTo(address: String, system: String, message: js.Any): Unit = {
    if (address == "")
      throw new IllegalArgumentException("Address must not be empty")
    if (system == "")
      throw new IllegalArgumentException("System must not be empty")

    onInitialized {
      if (address == myAddress) {
        postLocalMessageTo(system, message)
      } else {
        val data = js.Dynamic.literal(
            isWebWorkerRouterPostMessage = true,
            address = address,
            system = system,
            message = message)
        forwardMessage(data)
      }
    }
  }

  /** Posts a message to a system on the same router.
   *  This works even when the router is not initialized.
   */
  def postLocalMessageTo(system: String, message: js.Any): Unit = {
    setTimeout(0) {
      deliverMessage(system, message)
    }
  }

  /** Posts a message to an actor system address. */
  def postMessageTo(address: Address, message: js.Any): Unit = {
    if (address.hasLocalScope)
      postLocalMessageTo(address.system, message)
    else
      ()//postMessageTo(address.worker.get, address.system, message)
  }

  /** Registers a worker as child of this one and returns its address.
   *  If the given worker has already been registered to this router,
   *  registerChild does nothing and returns the already attributed address of
   *  that worker.
   *  Requires this worker to be initialized.
   */
  def registerChild(worker: Worker): String = {
    assertInitialized()

    children collectFirst {
      case (name, w) if w eq worker => myAddress + name + "/"
    } match {
      case Some(address) => address
      case None => registerNewChild(worker)
    }
  }

  /** Creates a child worker with the given script and registers it.
   *  Requires this worker to be initialized.
   */
  def createChild(script: String): String = {
    assertInitialized()
    val worker = new Worker(script)
    registerNewChild(worker)
  }

  private def registerNewChild(worker: Worker): String = {
    worker.addEventListener("message", { (event: js.Object) =>
      val data = event.asInstanceOf[MessageEvent].data
      global.console.log("Parent WebWorkerRouter receives:", data)
      if (!(!data.isWebWorkerRouterPostMessage))
        forwardMessage(data)
    }, useCapture = false)

    val childName = nextChildAddress.toString
    nextChildAddress += 1

    children(childName) = worker

    val address = myAddress + childName + "/"
    val initializeData = js.Dynamic.literal(
        isWebWorkerRouterInitialize = true,
        address = address)
    global.console.log("Parent WebWorkerRouter sends:", initializeData)
    worker.postMessage(initializeData)
    address
  }

  private[akka] def registerSystem(name: String,
      system: ActorSystemImpl): Unit = {
    systems(name) = system
  }

  private def initializeWithAddress(address: String): Unit = {
    if (isInitialized)
      throw new IllegalStateException(
          "Cannot initialize an already initialized router")

    myAddress = address
    val events = onInitializedEvents.reverse
    onInitializedEvents = Nil
    for (event <- events)
      event()
  }

  private def forwardMessage(data: js.Dynamic): Unit = {
    val address: String = data.address.asInstanceOf[String]

    if (address == myAddress) {
      // Arrived at destination
      deliverMessage(data.system.asInstanceOf[String], data.message)
    } else if (address.startsWith(myAddress)) {
      // Destination is one of my descendants
      val childName = address.substring(myAddress.length).split("/")(0)
      val childWorker = children(childName)
      global.console.log("Parent WebWorkerRouter sends:", data)
      childWorker.postMessage(data)
    } else {
      // Destination is not one of my descendants - forward to my parent
      global.console.log("Child WebWorkerRouter sends: " + js.JSON.stringify(data))
      ParentWorkerConnection.postMessage(data)
    }
  }

  private def deliverMessage(system: String, message: js.Any): Unit = {
    val sys = systems(system)
    //sys.deliverMessageFromRouter(message.asInstanceOf[js.Dynamic])
  }
}
