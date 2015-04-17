enablePlugins(ScalaJSPlugin)

name := "Scala.js actors"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

lazy val root = project.in(file(".")).
  aggregate(akkaActorJS, akkaActorJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val akkaActor = crossProject.in(file(".")).
  settings(
    name := "akka-js-actor",
    organization := "akka.js",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.6"
  ).
  jvmSettings(
    // stuff
  ).
  jsSettings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.8.0",
      "be.doeraene" %%% "scalajs-pickling" % "0.4.0",
      "com.lihaoyi" %%% "utest" % "0.3.1"
    )
  )

testFrameworks += new TestFramework("utest.runner.Framework")

preLinkJSEnv := PhantomJSEnv().value

postLinkJSEnv := PhantomJSEnv().value


lazy val akkaActorJVM = akkaActor.jvm
lazy val akkaActorJS = akkaActor.js




