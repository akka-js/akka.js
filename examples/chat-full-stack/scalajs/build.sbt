enablePlugins(ScalaJSPlugin)

scalaVersion := "2.11.6"

name := "Scala.js actors examples - chat client"

normalizedName := "scalajs-actors-example-chat-client"

libraryDependencies += "scala-js-actors" %%% "scala-js-actors" % "0.1-SNAPSHOT"

libraryDependencies += "be.doeraene" %%% "scalajs-jquery" % "0.7.0"
//"org.scala-lang.modules.scalajs" %%% "scalajs-jquery" % "0.6"
