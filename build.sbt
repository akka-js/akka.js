val akkaJsVersion = "0.1.3-SNAPSHOT"
val commonSettings = Seq(
    scalaVersion := "2.11.8",
    organization := "eu.unicredit",
    scalacOptions ++= Seq(
        "-deprecation",
        "-unchecked",
        "-feature",
        "-language:postfixOps",
        "-language:reflectiveCalls",
        "-encoding", "utf8"
    ),
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "sonatype-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
    scalaJSStage in Global := FastOptStage,
    cancelable in Global := true
)

val publishSettings = Seq(
  publishMavenStyle := true,
  pomIncludeRepository := { x => false },
  credentials += Credentials(Path.userHome / ".ivy2" / "sonatype.credentials"),
  pomExtra := {
    <url>https://github.com/unicredit/akka.js</url>
      <licenses>
        <license>
          <name>Scala License</name>
          <url>http://www.scala-lang.org/license.html</url>
        </license>
      </licenses>
      <scm>
        <connection>scm:git:github.com/unicredit/akka.js</connection>
        <developerConnection>scm:git:git@github.com:unicredit/akka.js</developerConnection>
        <url>github.com/unicredit/akka.js</url>
      </scm>
      <developers>
        <developer>
          <id>andreaTP</id>
          <name>Andrea Peruffo</name>
          <url>https://github.com/andreaTP/</url>
        </developer>
        <developer>
          <id>yawnt</id>
          <name>Gianluca Stivan</name>
          <url>https://github.com/yawnt/</url>
        </developer>
      </developers>
  }
)

lazy val akkaJsActor = crossProject.in(file("akka-js-actor"))
  .settings(commonSettings : _*)
  .settings(
    version := akkaJsVersion
  ).jsSettings(
    libraryDependencies ++= Seq(
      "eu.unicredit" %%% "shocon" % "0.0.2-SNAPSHOT",
      "org.scala-js" %%% "scalajs-java-time" % "0.1.0"
    ),
    compile in Compile := {
      val analysis = (compile in Compile).value
      val classDir = (classDirectory in Compile).value
      val configFile = (baseDirectory in Compile).value / ".." / ".." / "config" / "ir_patch.config"

      unicredit.IrPatcherPlugin.patchThis(classDir, configFile)

      analysis
    }
  ).jsSettings(
    useAnnotationAdderPluginSettings : _*
  ).jsSettings(
    publishSettings : _*
  ).jsSettings(sonatypeSettings : _*
  ).enablePlugins(spray.boilerplate.BoilerplatePlugin)

lazy val akkaJsActorJS = akkaJsActor.js

lazy val akkaTestkit = crossProject.in(file("akka-js-testkit"))
  .settings(commonSettings: _*)
  .settings(
    version := akkaJsVersion
  ).jsSettings(
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0-M16-SNAP6" withSources (),
    libraryDependencies += "org.scala-js" %% "scalajs-test-interface" % "0.6.10-SNAPSHOT" % "test",
    scalaJSStage in Global := FastOptStage,
    publishArtifact in (Test, packageBin) := true,
    scalaJSUseRhino in Global := false,
    preLinkJSEnv := NodeJSEnv().value,
    postLinkJSEnv := NodeJSEnv().value.withSourceMap(true)
  ).dependsOn(akkaJsActor)

lazy val akkaTestkitJS = akkaTestkit.js.dependsOn(akkaJsActorJS)

lazy val akkaActorTest = crossProject.in(file("akka-js-actor-tests"))
  .settings(commonSettings: _*)
  .settings(
    version := akkaJsVersion
    //parallelExecution in Test := false
  ).jsSettings(
    scalaJSUseRhino in Global := false,
    scalaJSStage in Global := FastOptStage,
    publishArtifact in (Test, packageBin) := true,
    //scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) },
    preLinkJSEnv := NodeJSEnv().value,
    postLinkJSEnv := NodeJSEnv().value.withSourceMap(true),
    libraryDependencies ++= Seq(
     "org.scalacheck" %%% "scalacheck" % "1.13.2" % "test",
     "io.megl" %%% "play-json-extra" % "2.4.3"
   )
 ).dependsOn(akkaTestkit % "test->test")

lazy val akkaActorTestJS = akkaActorTest.js

lazy val akkaJsActorStream = crossProject.in(file("akka-js-actor-stream"))
  .settings(commonSettings : _*)
  .settings(
    version := akkaJsVersion
  ).jsSettings(
  libraryDependencies ++= Seq(
    "eu.unicredit" %%% "shocon" % "0.0.2-SNAPSHOT",
    "org.scala-js" %%% "scalajs-dom" % "0.9.0",
    "org.scala-js" %%% "scalajs-java-time" % "0.1.0",
    "org.scala-lang.modules" %% "scala-java8-compat" % "0.7.0" % "provided"
  )
).jsSettings(
  useAnnotationAdderPluginSettings : _*
).jsSettings(
  publishSettings : _*
).dependsOn(akkaJsActor
).jsSettings(sonatypeSettings : _*
).enablePlugins(spray.boilerplate.BoilerplatePlugin)

lazy val akkaJsActorStreamJS = akkaJsActorStream.js

lazy val akkaStreamTestkit = crossProject.in(file("akka-js-stream-testkit"))
  .settings(commonSettings: _*)
  .settings(
    version := akkaJsVersion
  ).jsSettings(
  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0-M16-SNAP6" withSources (),
  libraryDependencies += "org.scala-js" %% "scalajs-test-interface" % "0.6.10-SNAPSHOT" % "test",
  scalaJSStage in Global := FastOptStage,
  publishArtifact in (Test, packageBin) := true,
  scalaJSUseRhino in Global := false,
  preLinkJSEnv := NodeJSEnv().value,
  postLinkJSEnv := NodeJSEnv().value.withSourceMap(true)
).dependsOn(akkaJsActorStream,akkaTestkit)

lazy val akkaStreamTestkitJS = akkaStreamTestkit.js

lazy val akkaStreamTest = crossProject.in(file("akka-js-stream-tests"))
  .settings(commonSettings: _*)
  .settings(
    version := akkaJsVersion
  ).jsSettings(
    scalaJSUseRhino in Global := false,
    scalaJSStage in Global := FastOptStage,
    publishArtifact in (Test, packageBin) := true,
    //scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) },
    preLinkJSEnv := NodeJSEnv().value,
    postLinkJSEnv := NodeJSEnv().value.withSourceMap(true),
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.0.0-M16-SNAP6" % "test",
      "org.scalacheck" %%% "scalacheck" % "1.13.2" % "test",
      "org.scala-lang.modules" %% "scala-java8-compat" % "0.7.0" % "provided"
   )
 ).dependsOn(akkaStreamTestkit % "test->test", akkaJsActorStream)

lazy val akkaStreamTestJS = akkaStreamTest.js

//COMPILER PLUGINS SECTION

//add scala.js annotations to proper classes
lazy val annotationAdderPlugin = Project(
    id   = "annotationAdderPlugin",
    base = file("plugins/annotation-adder-plugin")
  ) settings (
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
    publishArtifact in Compile := false
  ) settings (commonSettings : _*)

lazy val useAnnotationAdderPluginSettings = Seq(
    scalacOptions in Compile <++= (Keys.`package` in (annotationAdderPlugin, Compile)) map { (jar: File) =>
       Seq("-Xplugin:" + jar.getAbsolutePath)
    }
  )

//SCALAJS IR PATCHER SECTION

//core patches project
lazy val akkaJsActorIrPatches = Project(
    id   = "akkaActorJSIrPatches",
    base = file("akka-js-actor-ir-patches")
   ).
   settings (
    compile in Compile := {
      val analysis = (compile in Compile).value
      val classDir = (classDirectory in Compile).value
      val base = (baseDirectory in Compile).value

      val writer = new java.io.PrintWriter(base / ".." / "config" / "ir_patch.config", "UTF-8")
      writer.print(classDir)
      writer.flush
      writer.close
      analysis
    },
    publishArtifact in Compile := true
  ).settings (commonSettings : _*
  ).enablePlugins (ScalaJSPlugin)



lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(akkaJsActorIrPatches, akkaJsActorJS, akkaTestkitJS, akkaActorTestJS, akkaJsActorStreamJS, akkaStreamTestkitJS, akkaStreamTestJS)
