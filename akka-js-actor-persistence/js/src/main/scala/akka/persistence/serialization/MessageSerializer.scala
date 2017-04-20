/**
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.persistence.serialization

import akka.actor.{ ActorPath, ExtendedActorSystem }
import akka.persistence.AtLeastOnceDelivery._
import akka.persistence._
import akka.persistence.fsm.PersistentFSM.{ PersistentFSMSnapshot, StateChangeEvent }
import akka.persistence.serialization.{ MessageFormats ⇒ mf }
import akka.serialization._
//import akka.protobuf._
import _root_.com.google.protobuf.ByteString
import scala.collection.immutable.VectorBuilder
import scala.concurrent.duration
import akka.actor.Actor
import scala.concurrent.duration.Duration
import scala.language.existentials
import java.io.NotSerializableException

/**
 * Marker trait for all protobuf-serializable messages in `akka.persistence`.
 */
trait Message extends Serializable

/**
 * Protobuf serializer for [[akka.persistence.PersistentRepr]], [[akka.persistence.AtLeastOnceDelivery]] and [[akka.persistence.fsm.PersistentFSM.StateChangeEvent]] messages.
 */
class MessageSerializer(val system: ExtendedActorSystem) extends BaseSerializer {
  import PersistentRepr.Undefined

  val AtomicWriteClass = classOf[AtomicWrite]
  val PersistentReprClass = classOf[PersistentRepr]
  val PersistentImplClass = classOf[PersistentImpl]
  val AtLeastOnceDeliverySnapshotClass = classOf[AtLeastOnceDeliverySnapshot]
  val PersistentStateChangeEventClass = classOf[StateChangeEvent]
  val PersistentFSMSnapshotClass = classOf[PersistentFSMSnapshot[Any]]

  private lazy val serialization = SerializationExtension(system)

  override val includeManifest: Boolean = true

  private lazy val transportInformation: Option[Serialization.Information] = {
    val address = system.provider.getDefaultAddress
    if (address.hasLocalScope) None
    else Some(Serialization.Information(address, system))
  }

  /**
   * Serializes persistent messages. Delegates serialization of a persistent
   * message's payload to a matching `akka.serialization.Serializer`.
   */
  def toBinary(o: AnyRef): Array[Byte] = o match {
    case p: PersistentRepr              ⇒
      persistentMessageBuilder(p).toByteArray
    case a: AtomicWrite                 ⇒
      atomicWriteBuilder(a).toByteArray
    case a: AtLeastOnceDeliverySnapshot ⇒
      atLeastOnceDeliverySnapshotBuilder(a).toByteArray
    case s: StateChangeEvent            ⇒
      stateChangeBuilder(s).toByteArray
    case p: PersistentFSMSnapshot[Any]  ⇒
      persistentFSMSnapshotBuilder(p).toByteArray
    case _                              ⇒
      throw new IllegalArgumentException(s"Can't serialize object of type ${o.getClass}")
  }

  /**
   * Deserializes persistent messages. Delegates deserialization of a persistent
   * message's payload to a matching `akka.serialization.Serializer`.
   */
  def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): Message = manifest match {
    case None ⇒ persistent(mf.PersistentMessage.parseFrom(bytes))
    case Some(c) ⇒ c match {
      case PersistentImplClass              ⇒ persistent(mf.PersistentMessage.parseFrom(bytes))
      case PersistentReprClass              ⇒ persistent(mf.PersistentMessage.parseFrom(bytes))
      case AtomicWriteClass                 ⇒ atomicWrite(mf.AtomicWrite.parseFrom(bytes))
      case AtLeastOnceDeliverySnapshotClass ⇒ atLeastOnceDeliverySnapshot(mf.AtLeastOnceDeliverySnapshot.parseFrom(bytes))
      case PersistentStateChangeEventClass  ⇒ stateChange(mf.PersistentStateChangeEvent.parseFrom(bytes))
      case PersistentFSMSnapshotClass       ⇒ persistentFSMSnapshot(mf.PersistentFSMSnapshot.parseFrom(bytes))
      case _                                ⇒ throw new NotSerializableException(s"Can't deserialize object of type ${c}")
    }
  }

  //
  // toBinary helpers
  //

  def atLeastOnceDeliverySnapshotBuilder(snap: AtLeastOnceDeliverySnapshot): mf.AtLeastOnceDeliverySnapshot = {
    mf.AtLeastOnceDeliverySnapshot(
      currentDeliveryId = snap.currentDeliveryId,
      unconfirmedDeliveries = snap.unconfirmedDeliveries.map { unconfirmed ⇒
          mf.AtLeastOnceDeliverySnapshot.UnconfirmedDelivery(
            deliveryId = unconfirmed.deliveryId,
            destination = unconfirmed.destination.toString,
            payload = persistentPayloadBuilder(unconfirmed.message.asInstanceOf[AnyRef])
          )
      }
    )
  }

  private[persistence] def stateChangeBuilder(stateChange: StateChangeEvent): mf.PersistentStateChangeEvent = {
    mf.PersistentStateChangeEvent(
      stateIdentifier = stateChange.stateIdentifier,
      timeoutNanos = stateChange.timeout match {
        case None          ⇒ None
        case Some(timeout) ⇒ Some(timeout.toNanos)
      }
    )
  }

  private[persistence] def persistentFSMSnapshotBuilder(persistentFSMSnapshot: PersistentFSMSnapshot[Any]): mf.PersistentFSMSnapshot = {
    mf.PersistentFSMSnapshot(
      stateIdentifier = persistentFSMSnapshot.stateIdentifier,
      data = persistentPayloadBuilder(persistentFSMSnapshot.data.asInstanceOf[AnyRef]),
      timeoutNanos = persistentFSMSnapshot.timeout match {
        case None          ⇒ None
        case Some(timeout) ⇒ Some(timeout.toNanos)
      }
    )
  }

  def atLeastOnceDeliverySnapshot(atLeastOnceDeliverySnapshot: mf.AtLeastOnceDeliverySnapshot): AtLeastOnceDeliverySnapshot = {
    import scala.collection.JavaConverters._
    val unconfirmedDeliveries = new VectorBuilder[UnconfirmedDelivery]()
    atLeastOnceDeliverySnapshot.unconfirmedDeliveries foreach { next ⇒
      unconfirmedDeliveries += UnconfirmedDelivery(next.deliveryId, ActorPath.fromString(next.destination),
        payload(next.payload))
    }

    AtLeastOnceDeliverySnapshot(
      atLeastOnceDeliverySnapshot.currentDeliveryId,
      unconfirmedDeliveries.result())
  }

  def stateChange(persistentStateChange: mf.PersistentStateChangeEvent): StateChangeEvent = {
    StateChangeEvent(
      persistentStateChange.stateIdentifier,
      // timeout field is deprecated, left for backward compatibility. timeoutNanos is used instead.
      if (persistentStateChange.timeoutNanos.isDefined) Some(Duration.fromNanos(persistentStateChange.timeoutNanos.get))
      else if (persistentStateChange.timeout.isDefined) Some(Duration(persistentStateChange.timeout.get).asInstanceOf[duration.FiniteDuration])
      else None)
  }

  def persistentFSMSnapshot(persistentFSMSnapshot: mf.PersistentFSMSnapshot): PersistentFSMSnapshot[Any] = {
    PersistentFSMSnapshot(
      persistentFSMSnapshot.stateIdentifier,
      payload(persistentFSMSnapshot.data),
      if (persistentFSMSnapshot.timeoutNanos.isDefined) Some(Duration.fromNanos(persistentFSMSnapshot.timeoutNanos.get)) else None)
  }

  private def atomicWriteBuilder(a: AtomicWrite) = {
    mf.AtomicWrite(
      payload = a.payload.map(persistentMessageBuilder)
    )
  }

  private def persistentMessageBuilder(persistent: PersistentRepr) = {
    val _persistenceId =
      if (persistent.persistenceId != Undefined)
        Some(persistent.persistenceId)
      else None
    val _sender =
      if (persistent.sender != Actor.noSender)
        Some(Serialization.serializedActorPath(persistent.sender))
      else None
    val _manifest =
      if (persistent.manifest != PersistentRepr.Undefined)
        Some(persistent.manifest)
      else None

    mf.PersistentMessage(
      persistenceId = _persistenceId,
      sender = _sender,
      manifest = _manifest,
      payload = Some(persistentPayloadBuilder(persistent.payload.asInstanceOf[AnyRef])),
      sequenceNr = Some(persistent.sequenceNr)
    )
  }

  private def persistentPayloadBuilder(payload: AnyRef) = {
    def payloadBuilder() = {
      val serializer = serialization.findSerializerFor(payload)

      val _payloadMan =
      serializer match {
        case ser2: SerializerWithStringManifest ⇒
          val manifest = ser2.manifest(payload)
          if (manifest != PersistentRepr.Undefined)
            Some(ByteString.copyFromUtf8(manifest))
          else None
        case _ ⇒
          if (serializer.includeManifest)
            Some(ByteString.copyFromUtf8(payload.getClass.getName))
          else None
      }

      mf.PersistentPayload(
        payload = ByteString.copyFrom(serializer.toBinary(payload)),
        serializerId = serializer.identifier,
        payloadManifest = _payloadMan
      )
    }

    // serialize actor references with full address information (defaultAddress)
    transportInformation match {
      case Some(ti) ⇒ Serialization.currentTransportInformation.withValue(ti) { payloadBuilder() }
      case None     ⇒ payloadBuilder()
    }
  }

  //
  // fromBinary helpers
  //

  private def persistent(persistentMessage: mf.PersistentMessage): PersistentRepr = {
    PersistentRepr(
      payload(persistentMessage.getPayload),
      persistentMessage.getSequenceNr,
      if (persistentMessage.persistenceId.isDefined) persistentMessage.persistenceId.get else Undefined,
      if (persistentMessage.manifest.isDefined) persistentMessage.manifest.get else Undefined,
      if (persistentMessage.deleted.isDefined) persistentMessage.deleted.get else false,
      if (persistentMessage.sender.isDefined) system.provider.resolveActorRef(persistentMessage.sender.get) else Actor.noSender,
      if (persistentMessage.writerUuid.isDefined) persistentMessage.writerUuid.get else Undefined)
  }

  private def atomicWrite(atomicWrite: mf.AtomicWrite): AtomicWrite = {
    //import scala.collection.JavaConverters._
    AtomicWrite(atomicWrite.payload.map(persistent)(collection.breakOut))
  }

  private def payload(persistentPayload: mf.PersistentPayload): Any = {
    val manifest = if (persistentPayload.payloadManifest.isDefined)
      persistentPayload.payloadManifest.get.toStringUtf8 else ""

    serialization.deserialize(
      persistentPayload.payload.toByteArray,
      persistentPayload.serializerId,
      manifest).get
  }

}
