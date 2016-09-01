package akka

import com.google.protobuf.ByteString

package object remote {

  implicit def fromGoogleToAkka(bs: com.google.protobuf.ByteString) =
    bs.asInstanceOf[akka.protobuf.ByteString]

  import akka.remote.WireFormats.SerializedMessage
  implicit class SerializedMessageHelper(m: SerializedMessage) {
    def getMessage() = m.message
    def getSerializerId() = m.serializerId
    def getMessageManifest() = m.messageManifest
    def hasMessageManifest() = m.messageManifest.isDefined

    def build() = m.update()

    def setMessage(msg: ByteString) =
      m.withMessage(msg)
    def setSerializerId(id: Int) =
      m.withSerializerId(id)
    def setMessageManifest(bs: ByteString) =
      m.withMessageManifest(bs)
  }

  implicit class SerializedMessageStaticHelper(m: SerializedMessage.type) {
    def newBuilder = SerializedMessage()
  }

}
