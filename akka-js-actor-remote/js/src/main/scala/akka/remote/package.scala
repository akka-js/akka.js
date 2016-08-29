package akka

package object remote {
  //typeclasses to bind pojo methods.

  import akka.remote.WireFormats.SerializedMessage
  implicit class SerializedMessageHelper(m: SerializedMessage) {
    def getMessage() = m.message
    def getSerializerId() = m.serializerId
    def getMessageManifest() = m.messageManifest
    def hasMessageManifest() = m.messageManifest.isDefined
  }

}
