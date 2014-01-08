import ScalaJSKeys._

scalaJSSettings

scalaVersion := "2.11.0-M7"

name := "Scala.js actors"

scalaJSTestFramework in Test := "org.scalajs.actors.test.ActorsTestFramework"

scalaJSTestBridgeClass in Test := "org.scalajs.actors.test.ActorsTestBridge"
