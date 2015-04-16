package akka.scalajs.jsapi

import scala.scalajs.js

trait WorkerConnection extends org.scalajs.dom.EventTarget {
  def postMessage(message: js.Any): Unit = js.native

  var onmessage: js.Function1[MessageEvent, _] = js.native
}

class Worker(url: String) extends WorkerConnection {
  def terminate(): Unit = js.native
}

object ParentWorkerConnection extends WorkerConnection with js.GlobalScope
