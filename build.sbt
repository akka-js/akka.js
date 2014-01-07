import ScalaJSKeys._

// Turn this project into a Scala.js project by importing these settings
scalaJSSettings

organization := "org.scalajs"

name := "Scala.js actors"

normalizedName := "scalajs-actors"

version := "0.1-SNAPSHOT"

scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-encoding", "utf8"
)

scalaJSTestFramework in Test := "org.scalajs.actors.test.ActorsTestFramework"

scalaJSTestBridgeClass in Test := "org.scalajs.actors.test.ActorsTestBridge"
