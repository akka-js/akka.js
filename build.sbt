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
    scalaJSStage in Global := FastOptStage,
    cancelable in Global := true
)

lazy val akkaJsActor = crossProject.in(file("akka-js-actor"))
  .settings(commonSettings : _*)
  .settings(
    version := "0.1.1-SNAPSHOT"
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
  ).jsSettings(sonatypeSettings : _*)

lazy val akkaJsActorJS = akkaJsActor.js.dependsOn(akkaJsActorIrPatches % Compile)

lazy val akkaTestkit = crossProject.in(file("akka-js-testkit"))
  .settings(commonSettings: _*)
  .settings(
    version := "0.1.0-SNAPSHOT"
  ).jsSettings(
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0-M16-SNAP6",
    libraryDependencies += "org.scala-js" %% "scalajs-test-interface" % "0.6.10-SNAPSHOT" % "test",
    scalaJSStage in Global := FastOptStage,
    scalaJSUseRhino in Global := false,
    preLinkJSEnv := NodeJSEnv().value,
    postLinkJSEnv := NodeJSEnv().value.withSourceMap(true)
  ).dependsOn(akkaJsActor)

lazy val akkaTestkitJS = akkaTestkit.js.dependsOn(akkaJsActorJS)

lazy val akkaActorTest = crossProject.in(file("akka-js-actor-tests"))
  .settings(commonSettings: _*)
  .settings(
    version := "0.1.0-SNAPSHOT"
  ).jsSettings(
    preLinkJSEnv := NodeJSEnv().value,
    postLinkJSEnv := NodeJSEnv().value.withSourceMap(true),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % "1.12.2" % "test"
   )
 ).dependsOn(akkaTestkit)

lazy val akkaActorTestJS = akkaActorTest.js

lazy val akkaJsActorStream = crossProject.in(file("akka-js-actor-stream"))
  .settings(commonSettings : _*)
  .settings(
    version := "0.1.1-SNAPSHOT"
  ).jsSettings(
  libraryDependencies ++= Seq(
    "eu.unicredit" %%% "shocon" % "0.0.2-SNAPSHOT",
    "org.scala-js" %%% "scalajs-dom" % "0.9.0",
    "org.scala-js" %%% "scalajs-java-time" % "0.1.0",
    "org.reactivestreams" %%% "reactive-streams-scalajs" % "1.0.0",
    "org.scala-lang.modules" %% "scala-java8-compat" % "0.7.0"
  ),
  compile in Compile := {
    val analysis = (compile in Compile).value
    //val classDir = (classDirectory in Compile).value
    //val configFile = (baseDirectory in Compile).value / ".." / ".." / "config" / "ir_patch.config"

    //unicredit.IrPatcherPlugin.patchThis(classDir, configFile)

    analysis
  }
).jsSettings(
  useAnnotationAdderPluginSettings : _*
).jsSettings(
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
).dependsOn(akkaJsActor
).jsSettings(sonatypeSettings : _*
).enablePlugins(spray.boilerplate.BoilerplatePlugin)


lazy val akkaJsActorStreamJS = akkaJsActorStream.js

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
  ) settings (commonSettings : _*
  ) enablePlugins (ScalaJSPlugin)

lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(akkaJsActorIrPatches, akkaJsActorJS, akkaTestkitJS, akkaActorTestJS, akkaJsActorStreamJS)

