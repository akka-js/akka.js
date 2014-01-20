import ScalaJSKeys._

scalaJSSettings

scalaVersion := "2.11.0-M7"

name := "Scala.js actors"

libraryDependencies += "org.scalajs" %% "scalajs-pickling-core" % "0.1-SNAPSHOT"

scalaJSTestFramework in Test := "org.scalajs.actors.test.ActorsTestFramework"

scalaJSTestBridgeClass in Test := "org.scalajs.actors.test.ActorsTestBridge"
