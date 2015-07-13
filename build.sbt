val commonSettings = Seq(
    EclipseKeys.useProjectId := true,
    EclipseKeys.skipParents in ThisBuild := false,
    scalaVersion := "2.11.7",
    organization := "akka.js",
    scalacOptions ++= Seq(
        "-deprecation",
        "-unchecked",
        "-feature",
        "-encoding", "utf8"
    ),
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    scalaJSStage in Global := FastOptStage
)

lazy val akkaActor = crossProject.in(file("akka-js-actor"))
  .settings(commonSettings: _*)
  .settings(
    version := "0.2-SNAPSHOT"
  )
  .jvmSettings()
  .jsSettings( 
    unmanagedSourceDirectories in Compile += baseDirectory.value / "../../akka-js-testkit/js/src",
    libraryDependencies ++= Seq(
      "com.github.benhutchison" %%% "prickle" % "1.1.5",
      "be.doeraene" %%% "scalajs-pickling" % "0.4.0",
      "org.scalatest" %%% "scalatest" % "3.0.0-M1",
      "org.scala-js" %%% "scalajs-dom" % "0.8.0"
    )
  )

lazy val akkaTestkit = crossProject.in(file("akka-js-testkit"))
  .settings(commonSettings: _*)
  .settings(
    version := "0.2-SNAPSHOT"
  )
  .jvmSettings()
  .jsSettings(
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0-M1"
  )

lazy val akkaActorTest = crossProject.in(file("akka-js-actor-tests"))
  .settings(commonSettings: _*)
  .settings(
    version := "0.2-SNAPSHOT"
  )
  .jvmSettings(
  )
  .jsSettings(  
    scalaJSOptimizerOptions ~= { _.withBypassLinkingErrors(true) },
    preLinkJSEnv := NodeJSEnv().value,
    postLinkJSEnv := NodeJSEnv().value.withSourceMap(true),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % "1.12.2" % "test",
      "org.scalatest" %%% "scalatest" % "3.0.0-M1"
   )
  )

lazy val akkaWorkerMainJS = project.in(file("akka-js-worker/main"))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings: _*)
  .settings(
    version := "0.2-SNAPSHOT"
  )
  .dependsOn(akkaActor.js)

lazy val akkaWorkerRaftJS = project.in(file("akka-js-worker/raft"))
  .enablePlugins(ScalaJSPlugin)
  .settings(commonSettings: _*)
  .settings(
    version := "0.2-SNAPSHOT"
  )
  .dependsOn(akkaActor.js)

lazy val akkaActorJS = akkaActor.js

lazy val akkaTestkitJS = akkaTestkit.js.dependsOn(akkaActorJS)

lazy val akkaActorTestJS = akkaActorTest.js.dependsOn(akkaActorJS, akkaTestkitJS)

lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(akkaActorJS, akkaTestkitJS, akkaActorTestJS, akkaWorkerMainJS, akkaWorkerRaftJS)
