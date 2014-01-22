package models

import org.scalajs.spickling._

object RegisterPicklers {
  import PicklerRegistry.register

  register[User]
  register[Message]

  register[Join]
  register[SendMessage]
  register[ReceiveMessage]

  def registerPicklers(): Unit = ()
}
