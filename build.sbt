import ScalaJSKeys._

// Turn this project into a Scala.js project by importing these settings
scalaJSSettings

organization := "ch.epfl.lamp"

version := "0.1-SNAPSHOT"

name := "Scala.js actors"

version := "0.1-SNAPSHOT"

scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-feature",
    "-encoding", "utf8"
)

sources in (Test, packageJS) += baseDirectory.value / "js" / "runtests.js"
