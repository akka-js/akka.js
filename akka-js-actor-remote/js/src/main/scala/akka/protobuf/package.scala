package akka

package object protobuf {
  //Rebinding

  class ByteStringImpl {

    def copyFrom(bytes: Array[Byte]) =
      com.google.protobuf.ByteString.copyFrom(bytes)

    def copyFromUtf8(str: String) =
      com.google.protobuf.ByteString.copyFromUtf8(str)
  }

  trait ByteString extends com.google.protobuf.ByteString

  object ByteString extends ByteStringImpl

}
