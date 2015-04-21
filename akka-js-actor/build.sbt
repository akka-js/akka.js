enablePlugins(ScalaJSPlugin)

name := "Akka.js actors"

version := "0.2-SNAPSHOT"

normalizedName := "akka-js-actor"

organization := "akka.js"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.8.0",
  "be.doeraene" %%% "scalajs-pickling" % "0.4.0",
  "com.lihaoyi" %%% "utest" % "0.3.1"
)

preLinkJSEnv := NodeJSEnv().value

postLinkJSEnv := NodeJSEnv().value

testFrameworks += new TestFramework("utest.runner.Framework")
