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

  import akka.remote.WireFormats.DeployData
  implicit class DeployDataHelper(d: DeployData) {
    def setPath(p: String) = d.withPath(p)
    def setConfig(c: ByteString) = d.withConfig(c)
    def setRouterConfig(rc: ByteString) = d.withRouterConfig(rc)
    def setScope(s: ByteString) = d.withScope(s)
    def setDispatcher(s: String) = d.withDispatcher(s)

    def hasConfig() = d.config.isDefined
    def hasRouterConfig() = d.routerConfig.isDefined
    def hasScope() = d.scope.isDefined
    def hasDispatcher() = d.dispatcher.isDefined

    def getPath() = d.path

    def build() = d.update()
  }

  implicit class DeployDataStaticHelper(d: DeployData.type) {
    def newBuilder = DeployData()
  }

  import akka.remote.WireFormats.PropsData
  implicit class PropsDataHelper(p: PropsData) {
    def getClazz() = p.clazz

    import collection.JavaConverters._
    def getArgsList() = p.args.asJava
    def getClassesList() = p.classes.asJava

    def build() = p.update()
  }

  class PropsDataBuilder(var p: PropsData) {
    def setClazz(clz: String) = {
      p = p.withClazz(clz)
      this
    }
    def setDeploy(d: DeployData) = {
      p = p.withDeploy(d)
      this
    }
    def addArgs(a: ByteString) = {
      p = p.withArgs(p.args :+ a)
      this
    }
    def addClasses(clz: String) = {
      p = p.withClasses(p.classes :+ clz)
      this
    }

    def build() = p.update()
  }

  implicit class PropsDataStaticHelper(d: PropsData.type) {
    def newBuilder = new PropsDataBuilder(PropsData())
  }

  import akka.remote.WireFormats.DaemonMsgCreateData
  import akka.remote.WireFormats.ActorRefData
  implicit class DaemonMsgCreateDataHelper(d: DaemonMsgCreateData) {
    def setProps(p: PropsData) = d.withProps(p)
    def setDeploy(dep: DeployData) = d.withDeploy(dep)
    def setPath(p: String) = d.withPath(p)
    def setSupervisor(s: ActorRefData) = d.withSupervisor(s)

    def getProps() = d.props

    def build() = d.update()
  }

  implicit class DaemonMsgCreateDataStaticHelper(d: DaemonMsgCreateData.type) {
    def newBuilder = DaemonMsgCreateData()
  }

  object AkkaHandshakeInfo {

    type Builder = akka.remote.WireFormats.AkkaHandshakeInfo
  }
}
