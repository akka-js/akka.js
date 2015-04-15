enablePlugins(ScalaJSPlugin)

name := "Scala.js actors"

normalizedName := "akka-js-actors"

organization := "akka.js"

libraryDependencies ++= Seq(
  "be.doeraene" %%% "scalajs-pickling" % "0.4.0",
  "com.lihaoyi" %%% "utest" % "0.3.0" % "test"
)

testFrameworks += new TestFramework("org.scalajs.actors.test.ActorsTestFramework")
