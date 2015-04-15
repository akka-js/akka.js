name := "Scala.js - Akka Websocket Bridge"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

lazy val root = project.in(file(".")).
  aggregate(akkaWebSocketJS, akkaWebSocketJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val akkaWebSocket = crossProject.in(file(".")).
  settings(
    name := "akka-js-websocket",
    organization := "akka.js",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.6"
  ).
  jvmSettings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.3.9",
      "com.typesafe.play" % "play_2.11" % "2.3.8",
      "be.doeraene" %% "scalajs-pickling-play-json" % "0.4.0"
    )
  ).
  jsSettings(
    libraryDependencies ++= Seq(
      "scala-js-actors" %%% "scala-js-actors" % "0.1-SNAPSHOT",
      "be.doeraene" %%% "scalajs-pickling" % "0.4.0"
    )    
  )

lazy val akkaWebSocketJVM = akkaWebSocket.jvm
lazy val akkaWebSocketJS = akkaWebSocket.js
