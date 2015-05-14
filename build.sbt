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

lazy val akkaActorTest = crossProject.in(file("akka-js-actor-tests"))
  .settings(commonSettings: _*)
  .settings(
    version := "0.2-SNAPSHOT",
    normalizedName := "akka-js-actor-tests"
  )
  .jvmSettings(
  )
  .jsSettings(  
    scalaJSOptimizerOptions ~= { _.withBypassLinkingErrors(true) },
    unmanagedSourceDirectories in Compile += baseDirectory.value / "../../akka-js-actor/js/src",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "../../akka-js-actor/shared/src",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "../../akka-js-testkit/js/src",
    preLinkJSEnv := NodeJSEnv().value,
    postLinkJSEnv := NodeJSEnv().value.withSourceMap(true),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % "1.12.2" % "test",
      "org.scalatest" %%% "scalatestjs" % "2.3.0-SNAP2"
   )
  )

lazy val akkaActorTestJS = akkaActorTest.js

lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(akkaActorTestJS)
