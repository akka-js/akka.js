import ScalaJSKeys._

scalaJSSettings

scalaVersion := "2.11.0-M7"

name := "Scala.js simple pickling"

libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scala-lang.modules.scalajs" %% "scalajs-jasmine-test-framework" % scalaJSVersion % "test"
)
