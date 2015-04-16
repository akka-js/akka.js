enablePlugins(ScalaJSPlugin)

name := "Scala.js actors"

normalizedName := "akka-js-actor"

organization := "akka.js"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "be.doeraene" %%% "scalajs-pickling" % "0.4.0",
  "com.lihaoyi" %%% "utest" % "0.3.1"
)

testFrameworks += new TestFramework("utest.runner.Framework")