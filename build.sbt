val commonSettings = Seq(
    EclipseKeys.useProjectId := true,
    scalaVersion := "2.11.6",
    organization := "akka.js",
    scalacOptions ++= Seq(
        "-deprecation",
        "-unchecked",
        "-feature",
        "-encoding", "utf8"
    ),
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

lazy val akkaActor = crossProject.in(file("akka-js-actor"))
  .settings(commonSettings: _*)
  .settings(
    version := "0.2-SNAPSHOT"
  )
  .jvmSettings()
  .jsSettings( 
    unmanagedSourceDirectories in Compile += baseDirectory.value / "../../akka-js-testkit/js/src",
    libraryDependencies += "org.scalatest" %%% "scalatestjs" % "2.3.0-SNAP2"
  )

lazy val akkaTestkit = crossProject.in(file("akka-js-testkit"))
  .settings(commonSettings: _*)
  .settings(
    version := "0.2-SNAPSHOT"
  )
  .jvmSettings()
  .jsSettings(
    libraryDependencies += "org.scalatest" %%% "scalatestjs" % "2.3.0-SNAP2"
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
      "org.scalatest" %%% "scalatestjs" % "2.3.0-SNAP2"
   )
  )


lazy val akkaActorJS = akkaActor.js

lazy val akkaTestkitJS = akkaTestkit.js.dependsOn(akkaActorJS)

lazy val akkaActorTestJS = akkaActorTest.js.dependsOn(akkaActorJS, akkaTestkitJS)

lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(akkaActorJS, akkaTestkitJS, akkaActorTestJS)
