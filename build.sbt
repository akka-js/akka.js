val commonSettings = Seq(
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
  .jvmSettings(
    libraryDependencies += "com.typesafe" % "config" % "1.3.0"
  )
  .jsSettings(
    unmanagedSourceDirectories in Compile += baseDirectory.value / "../../akka-js-testkit/js/src",
    libraryDependencies ++= Seq(
      "eu.unicredit" %%% "shocon" % "0.0.1-SNAPSHOT",
      "com.github.benhutchison" %%% "prickle" % "1.1.5",
      "be.doeraene" %%% "scalajs-pickling" % "0.4.0",
      "org.scalatest" %%% "scalatest" % "3.0.0-M1",
      "org.scala-js" %%% "scalajs-dom" % "0.8.0"
    )
  ).jsSettings(
    useAnnotationAdderPluginSettings ++
    useMethodEraserPluginSettings : _*
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

lazy val akkaActorJVM = akkaActor.jvm

lazy val akkaTestkitJS = akkaTestkit.js.dependsOn(akkaActorJS)

lazy val akkaActorTestJS = akkaActorTest.js.dependsOn(akkaActorJS, akkaTestkitJS)

//COMPILER PLUGINS SECTION
lazy val annotationAdderPlugin = Project(
    id   = "annotationAdderPlugin",
    base = file("annotation-adder-plugin")
  ) settings (
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
    publishArtifact in Compile := false
  ) settings (commonSettings : _*)

lazy val useAnnotationAdderPluginSettings = Seq(
    scalacOptions in Compile <++= (Keys.`package` in (annotationAdderPlugin, Compile)) map { (jar: File) =>
       Seq("-Xplugin:" + jar.getAbsolutePath)
    }
  )

lazy val methodEraserPlugin = Project(
    id   = "methodEraserPlugin",
    base = file("method-eraser-plugin")
  ) settings (
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
    publishArtifact in Compile := false
  ) settings (commonSettings : _*)

lazy val useMethodEraserPluginSettings = Seq(
  scalacOptions in Compile <++= (Keys.`package` in (methodEraserPlugin, Compile)) map { (jar: File) =>
     Seq("-Xplugin:" + jar.getAbsolutePath)
  }
)

lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(akkaActorJS, akkaActorJVM, akkaTestkitJS, akkaActorTestJS, akkaWorkerMainJS, akkaWorkerRaftJS)
