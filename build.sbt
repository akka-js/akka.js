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
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

lazy val akkaTestkit = crossProject.in(file("akka-js-testkit"))
  .settings(commonSettings: _*)
  .settings(
    version := "0.2-SNAPSHOT",
    normalizedName := "akka-js-testkit"
  )
  .jvmSettings(
  )
  .jsSettings( 
    unmanagedSourceDirectories in Compile += baseDirectory.value / "../../akka-js-actor/js/src",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "../../akka-js-actor/shared/src",
    preLinkJSEnv := NodeJSEnv().value,
    postLinkJSEnv := NodeJSEnv().value.withSourceMap(true),
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatestjs" % "2.3.0-SNAP2",
      "org.scala-js" %%% "scalajs-dom" % "0.8.0",
      "be.doeraene" %%% "scalajs-pickling" % "0.4.0",
      "com.lihaoyi" %%% "utest" % "0.3.1" 
   )
  )

lazy val akkaTestkitJS = akkaTestkit.js
lazy val akkaTestkitJVM = akkaTestkit.jvm

lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(akkaTestkitJS)
