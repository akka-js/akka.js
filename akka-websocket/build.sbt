scalaVersion := "2.11.6"

name := "Scala.js-Akka Websocket Bridge"

normalizedName := "akka-websocket-bridge"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "main"/ "wscommon"

resolvers += Resolver.url("scala-js-releases",
    url("http://dl.bintray.com/content/scala-js/scala-js-releases"))(
    Resolver.ivyStylePatterns)

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.3.9",
    "com.typesafe.play" % "play_2.11" % "2.3.8",
    "be.doeraene" %% "scalajs-pickling-play-json" % "0.4.0"
)
