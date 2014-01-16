package org.scalajs.actors
package webworkers

import scala.scalajs.js
import org.scalajs.spickling._

object DedicatedPicklers {
  implicit object ChildActorPathPickler extends Pickler[ChildActorPath] {
    def pickle(path: ChildActorPath)(implicit registry: PicklerRegistry): js.Any = {
      val result = js.Object().asInstanceOf[js.Dynamic]
      result.root = registry.pickle(path.root)
      result.elements = js.Any.fromTraversableOnce(path.elements)
      result.uid = path.uid
      result
    }
  }

  implicit object ChildActorPathUnpickler extends Unpickler[ChildActorPath] {
    def unpickle(pickle: js.Any)(implicit registry: PicklerRegistry): ChildActorPath = {
      val dyn = pickle.asInstanceOf[js.Dynamic]
      val root = registry.unpickle(dyn.root).asInstanceOf[RootActorPath]
      val elements = dyn.elements.asInstanceOf[js.Array[String]].toList
      val uid = dyn.uid.asInstanceOf[js.Number].toInt
      (root / elements).withUid(uid).asInstanceOf[ChildActorPath]
    }
  }
}
