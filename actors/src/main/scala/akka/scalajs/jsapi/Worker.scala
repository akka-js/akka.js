package akka.scalajs.jsapi

import scala.scalajs.js

trait WorkerConnection extends EventTarget {
  def postMessage(message: js.Any): Unit = ???

  var onmessage: js.Function1[MessageEvent, _] = ???
}

class Worker(url: js.String) extends WorkerConnection {
  def terminate(): Unit = ???
}

object ParentWorkerConnection extends WorkerConnection with js.GlobalScope
