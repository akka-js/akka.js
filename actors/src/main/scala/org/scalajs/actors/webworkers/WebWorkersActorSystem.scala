package org.scalajs.actors
package webworkers

private[actors] trait WebWorkersActorSystem { this: ActorSystemImpl =>
  private var _workerPath: String = ""

  private var childWorkers = JSMap.empty[WorkerConnection]

  private[webworkers] def workerAddress = {
    assert(_workerPath != "")
    Address(name, _workerPath)
  }

  private[webworkers] def globalizePath(path: ActorPath): ActorPath = {
    if (path.address.hasGlobalScope) path
    else RootActorPath(workerAddress) / path.elements
  }

  private[webworkers] def computeNextHop(
      address: Address): Option[WorkerConnection] = {
    if (address.hasLocalScope) None
    else {
      assert(_workerPath != "")
      val destWorkerPath = address.worker.get
      if (destWorkerPath == _workerPath) {
        None
      } else if (destWorkerPath.startsWith(_workerPath)) {
        // next hop is one of my children
        val childName =
          destWorkerPath.substring(_workerPath.length).split("/")(0)
        Some(childWorkers.getOrElse(childName,
            throw new Exception(s"Cannot locate worker $destWorkerPath")))
      } else {
        // next hop is my parent
        Some(ParentWorkerConnection)
      }
    }
  }
}
