import ScalaJSKeys._

scalaJSSettings

name := "Scala.js actors"

libraryDependencies += "org.scalajs" %% "scalajs-pickling" % "0.1-SNAPSHOT"

scalaJSTestFramework in Test := "org.scalajs.actors.test.ActorsTestFramework"

scalaJSTestBridgeClass in Test := "org.scalajs.actors.test.ActorsTestBridge"
