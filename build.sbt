import ScalaJSKeys._

val commonSettings = Seq(
    organization := "org.scalajs",
    version := "0.1-SNAPSHOT",
    normalizedName ~= { _.replace("scala-js", "scalajs") },
    scalacOptions ++= Seq(
        "-deprecation",
        "-unchecked",
        "-feature",
        "-encoding", "utf8"
    )
)

lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(actors, akkaWebsocketBridge)

lazy val actors = project.settings(commonSettings: _*)

lazy val akkaWebsocketBridge = project.in(file("akka-websocket-bridge"))
  .settings(commonSettings: _*)

lazy val examples = project.settings(commonSettings: _*)
  .aggregate(webworkersExample, faultToleranceExample,
      chatExample, chatExampleScalaJS)

lazy val webworkersExample = project.in(file("examples/webworkers"))
  .settings(commonSettings: _*)
  .dependsOn(actors)

lazy val faultToleranceExample = project.in(file("examples/faulttolerance"))
  .settings(commonSettings: _*)
  .dependsOn(actors)

lazy val chatExample = project.in(file("examples/chat-full-stack"))
  .settings(commonSettings: _*)
  .dependsOn(akkaWebsocketBridge)

lazy val chatExampleScalaJS = project.in(file("examples/chat-full-stack/scalajs"))
  .settings(commonSettings: _*)
  .dependsOn(actors)
