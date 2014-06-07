scalaVersion := "2.10.4"

name := "Scala.js-Akka Websocket Bridge"

normalizedName := "akka-websocket-bridge"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += Resolver.url("scala-js-releases",
    url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(
    Resolver.ivyStylePatterns)

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3.3",
    "com.typesafe.play" %% "play" % "2.3.0",
    "org.scalajs" %% "scalajs-pickling-play-json" % "0.3"
)
