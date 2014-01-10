package org.scalajs.examples.webworkers

import scala.scalajs.js

trait MessageEvent extends js.Object {
  val data: js.Dynamic = ???
}

trait WebWorkerConnection extends js.Object {
  def postMessage(message: js.Any): Unit = ???

  var onmessage: js.Function1[MessageEvent, _] = ???
}

class WebWorker(url: js.String) extends WebWorkerConnection {
  def terminate(): Unit = ???
}

object ParentWebWorkerConnection extends WebWorkerConnection with js.GlobalScope
