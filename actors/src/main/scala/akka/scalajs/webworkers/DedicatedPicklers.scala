package akka.scalajs.webworkers

import org.scalajs.spickling._

import akka.actor._

object DedicatedPicklers {
  implicit object ChildActorPathPickler extends Pickler[ChildActorPath] {
    def pickle[P](path: ChildActorPath)(implicit registry: PicklerRegistry,
        builder: PBuilder[P]): P = {
      builder.makeObject(
          ("root", registry.pickle(path.root)),
          ("elements", builder.makeArray(
              path.elements.map(builder.makeString(_)).toSeq: _*)),
          ("uid", builder.makeNumber(path.uid)))
    }
  }

  implicit object ChildActorPathUnpickler extends Unpickler[ChildActorPath] {
    def unpickle[P](pickle: P)(implicit registry: PicklerRegistry,
        reader: PReader[P]): ChildActorPath = {
      val root = registry.unpickle(
          reader.readObjectField(pickle, "root")).asInstanceOf[ActorPath]
      val elemsArray = reader.readObjectField(pickle, "elements")
      val elements = (0 until reader.readArrayLength(elemsArray)).map {
        i => reader.readString(reader.readArrayElem(elemsArray, i))
      }
      val uid = reader.readNumber(reader.readObjectField(pickle, "uid")).toInt
      (root / elements).withUid(uid).asInstanceOf[ChildActorPath]
    }
  }
}
