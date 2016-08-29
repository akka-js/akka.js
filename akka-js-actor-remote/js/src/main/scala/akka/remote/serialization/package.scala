package akka.remote

package object serialization {

  implicit def fromGoogleToAkka(bs: com.google.protobuf.ByteString) =
    bs.asInstanceOf[akka.protobuf.ByteString]

}
