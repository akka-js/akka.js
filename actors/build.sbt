import ScalaJSKeys._

scalaJSSettings

name := "Scala.js actors"

scalaJSTestFramework in Test := "org.scalajs.actors.test.ActorsTestFramework"

scalaJSTestBridgeClass in Test := "org.scalajs.actors.test.ActorsTestBridge"
