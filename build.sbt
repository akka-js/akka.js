enablePlugins(ScalaJSPlugin)

val commonSettings = Seq(
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.6",
    organization := "akka.js",
    scalacOptions ++= Seq(
        "-deprecation",
        "-unchecked",
        "-feature",
        "-encoding", "utf8"
    )
)

lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(akkaActor)

lazy val akkaWebSocket = project.in(file("akka-websocket"))
  .settings(commonSettings: _*)

lazy val akkaActor = project.in(file("akka-actor"))
  .settings(commonSettings: _*)
