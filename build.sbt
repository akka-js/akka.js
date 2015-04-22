val commonSettings = Seq(
    EclipseKeys.useProjectId := true,
    scalaVersion := "2.11.6",
    organization := "akka.js",
    scalacOptions ++= Seq(
        "-deprecation",
        "-unchecked",
        "-feature",
        "-encoding", "utf8"
    ),
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

lazy val akkaActor = crossProject.in(file("akka-js-actor"))
  .settings(commonSettings: _*)
  .settings(
    name := "Akka.js actors",
    version := "0.2-SNAPSHOT",
    normalizedName := "akka-js-actor"
  )
  .jvmSettings()
  .jsSettings(
    preLinkJSEnv := NodeJSEnv().value,
    postLinkJSEnv := NodeJSEnv().value,
    testFrameworks += new TestFramework("utest.runner.Framework"),
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.8.0",
      "be.doeraene" %%% "scalajs-pickling" % "0.4.0",
      "com.lihaoyi" %%% "utest" % "0.3.1"
    )
  )

lazy val akkaWebSocket = crossProject.in(file("akka-js-websocket")).
  settings(commonSettings: _*).
  settings(
    name := "akka-js-websocket",
    version := "0.2-SNAPSHOT"
  ).
  jvmSettings(
    libraryDependencies ++= Seq( 
      "com.typesafe.akka" %% "akka-actor" % "2.3.9",
      "com.typesafe.play" % "play_2.11" % "2.4.0-M3",
      "be.doeraene" %% "scalajs-pickling-play-json" % "0.4.0"
    )
  ).
  jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.8.0",
      "akka.js" %%% "akka-js-actor" % "0.2-SNAPSHOT",
      "be.doeraene" %%% "scalajs-pickling" % "0.4.0"
    )    
  )

lazy val akkaActorJS = akkaActor.js

lazy val akkaWebSocketJVM = akkaWebSocket.jvm
lazy val akkaWebSocketJS = akkaWebSocket.js.dependsOn(akkaActorJS)

lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(akkaActorJS, akkaWebSocketJS, akkaWebSocketJVM)
