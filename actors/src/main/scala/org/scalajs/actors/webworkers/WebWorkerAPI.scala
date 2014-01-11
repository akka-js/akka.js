package org.scalajs.actors.webworkers

import scala.scalajs.js

trait MessageEvent extends js.Object {
  val data: js.Dynamic = ???
}

trait WorkerConnection extends js.Object {
  def postMessage(message: js.Any): Unit = ???

  var onmessage: js.Function1[MessageEvent, _] = ???

  def addEventListener(`type`: js.String,
      listener: js.Function1[js.Object, _],
      useCapture: js.Boolean): Unit = ???
  def removeEventListener(`type`: js.String,
      listener: js.Function1[js.Object, _],
      useCapture: js.Boolean): Unit = ???
}

class Worker(url: js.String) extends WorkerConnection {
  def terminate(): Unit = ???
}

object ParentWorkerConnection extends WorkerConnection with js.GlobalScope
