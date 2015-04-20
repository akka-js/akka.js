enablePlugins(ScalaJSPlugin)

name := "Scala.js actors"

normalizedName := "akka-js-actor"

organization := "akka.js"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.8.0",
  "be.doeraene" %%% "scalajs-pickling" % "0.4.0",
  "com.lihaoyi" %%% "utest" % "0.3.1"
)

preLinkJSEnv := PhantomJSEnv().value

postLinkJSEnv := PhantomJSEnv().value

testFrameworks += new TestFramework("utest.runner.Framework")
