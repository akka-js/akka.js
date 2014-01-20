scalaVersion := "2.10.3"

name := "Scala.js-Akka Websocket Bridge"

normalizedName := "akka-websocket-bridge"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.2.3",
    "com.typesafe.play" %% "play" % "2.2.1"
)
