package models

import be.doeraene.spickling._

object RegisterPicklers {
  import PicklerRegistry.register

  // Utils
  register(Nil)
  register[::[Any]]

  // Models
  register[User]
  register[Room]
  register[Message]

  // Actions from users
  register[Connect]
  register[Join]
  register(Leave)
  register[SendMessage]

  // Requests
  register[RequestPrivateChat]
  register(AcceptPrivateChat)
  register(RejectPrivateChat)
  register(UserDoesNotExist)

  // Notifications from server
  register[RoomListChanged]
  register[JoinedRoom]
  register[UserJoined]
  register[UserLeft]
  register[ReceiveMessage]

  def registerPicklers(): Unit = ()

  implicit object ConsPickler extends Pickler[::[Any]] {
    def pickle[P](value: ::[Any])(implicit registry: PicklerRegistry,
        builder: PBuilder[P]): P = {
      builder.makeArray(value.map(registry.pickle(_)): _*)
    }
  }

  implicit object ConsUnpickler extends Unpickler[::[Any]] {
    def unpickle[P](pickle: P)(implicit registry: PicklerRegistry,
        reader: PReader[P]): ::[Any] = {
      val len = reader.readArrayLength(pickle)
      assert(len > 0)
      ((0 until len).toList map { index =>
        registry.unpickle(reader.readArrayElem(pickle, index))
      }).asInstanceOf[::[Any]]
    }
  }
}
