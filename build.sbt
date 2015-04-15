enablePlugins(ScalaJSPlugin)

val commonSettings = Seq(
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.6",
    scalacOptions ++= Seq(
        "-deprecation",
        "-unchecked",
        "-feature",
        "-encoding", "utf8"
    )
)

lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(akkaActor, akkaWebsocket)

lazy val akkaActors = project.in("akka-actor")
  .settings(commonSettings: _*)
  .settings(
      unmanagedSourceDirectories in Compile +=
        (sourceDirectory in Compile).value / "wscommon"
  )

lazy val akkaWebsocket = project.in(file("akka-websocket"))
  .settings(commonSettings: _*)
  .settings(
      unmanagedSourceDirectories in Compile +=
        (sourceDirectory in (actors, Compile)).value / "wscommon"
  )

