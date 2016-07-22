val akkaJsVersion = "0.1.3-SNAPSHOT"
val akkaOriginalVersion = "v2.4.8"

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

lazy val akkaVersion = settingKey[String]("akkaVersion")

lazy val assembleAkkaLibrary = taskKey[Unit](
  "Checks out akka standard library from submodules/akka and then applies overrides.")

//basically eviction rules
def rm_clash(base: java.io.File, target: java.io.File): Unit = {
  if (base.exists &&
    ((base.isFile &&
      ((target.exists && target.isFile) || base.getName.endsWith(".java"))) ||
      (base.isDirectory && target.isDirectory &&
        IO.listFiles(target).filterNot(_.getName.startsWith(".")).isEmpty))
  ) {
    IO.delete(base)
  } else if (base.exists && base.isDirectory)
    IO.listFiles(base).foreach(f => rm_clash(f, new java.io.File(target, f.getName)))
}

lazy val akkaJsActor = crossProject.in(file("akka-js-actor"))
  .settings(commonSettings : _*)
  .settings(
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    assembleAkkaLibrary := {
      import org.eclipse.jgit.api._

      val s = streams.value
      val trgDir = target.value / "akkaSources" / akkaVersion.value

      if (!trgDir.exists) {
        s.log.info(s"Fetching Akka source version ${akkaVersion.value}")

        // Make parent dirs and stuff
        IO.createDirectory(trgDir)

        // Clone akka source code
        new CloneCommand()
          .setDirectory(trgDir)
          .setURI("https://github.com/akka/akka.git")
          .call()
      }

      // Checkout proper ref. We do this anyway so we fail if
      // something is wrong
      val git = Git.open(trgDir)
      s.log.info(s"Checking out Akka source version ${akkaVersion.value}")
      git.checkout().setName(s"${akkaVersion.value}").call()

      val srcTarget = file("akka-js-actor/shared/src/main/scala")
      IO.delete(srcTarget)
      IO.copyDirectory(
        trgDir / "akka-actor" / "src" / "main" / "scala",
        srcTarget, overwrite = true)

      val boilerplateTarget = file("akka-js-actor/js/src/main/boilerplate")
      IO.delete(boilerplateTarget)

      IO.copyDirectory(
        trgDir / "akka-actor" / "src" / "main" / "boilerplate",
        boilerplateTarget, overwrite = true)

      val jsSources = file("akka-js-actor/js/src/main/scala")

      rm_clash(srcTarget, jsSources)
    }
  ).jsSettings(
  libraryDependencies ++= Seq(
    "eu.unicredit" %%% "shocon" % "0.0.2-SNAPSHOT",
    "org.scala-js" %%% "scalajs-java-time" % "0.1.0",
    "org.scala-lang.modules" %% "scala-java8-compat" % "0.7.0" % "provided"
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
).jsSettings(
  excludeDependencies += ("eu.unicredit" %% "akkaactorjsirpatches"),
  compile in Compile <<= (compile in Compile) dependsOn assembleAkkaLibrary,
  publishLocal <<= publishLocal dependsOn assembleAkkaLibrary
).enablePlugins(spray.boilerplate.BoilerplatePlugin)

lazy val akkaJsActorJS = akkaJsActor.js.dependsOn(akkaJsActorIrPatches % "provided")

lazy val akkaTestkit = crossProject.in(file("akka-js-testkit"))
  .settings(commonSettings: _*)
  .settings(
    version := akkaJsVersion
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
    version := akkaJsVersion
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
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    assembleAkkaLibrary := {
      import org.eclipse.jgit.api._

      val s = streams.value
      val trgDir = target.value / "akkaSources" / akkaVersion.value


      val akkaActorTrg = (file("akka-js-actor/js/target/") / "akkaSources" / akkaVersion.value)

      if (!trgDir.exists) {
        if (akkaActorTrg.exists) {
          s.log.info(s"Akka sources from akkaActor sub-project")

          // Make parent dirs and stuff
          IO.createDirectory(trgDir)

          IO.copyDirectory(
            akkaActorTrg,
            trgDir, overwrite = true)

        } else {
          s.log.info(s"Fetching Akka source version ${akkaVersion.value}")

          // Make parent dirs and stuff
          IO.createDirectory(trgDir)

          // Clone akka source code
          new CloneCommand()
            .setDirectory(trgDir)
            .setURI("https://github.com/akka/akka.git")
            .call()
        }
      }

      // Checkout proper ref. We do this anyway so we fail if
      // something is wrong
      val git = Git.open(trgDir)
      s.log.info(s"Checking out Akka source version ${akkaVersion.value}")
      git.checkout().setName(s"${akkaVersion.value}").call()

      val srcTarget = file("akka-js-actor-stream/shared/src/main/scala")
      IO.delete(srcTarget)
      IO.copyDirectory(
        trgDir / "akka-stream" / "src" / "main" / "scala",
        srcTarget, overwrite = true)

      val boilerplateTarget = file("akka-js-actor-stream/js/src/main/boilerplate")
      IO.delete(boilerplateTarget)

      IO.copyDirectory(
        trgDir / "akka-stream" / "src" / "main" / "boilerplate",
        boilerplateTarget, overwrite = true)

      val jsSources = file("akka-js-actor-stream/js/src/main/scala")

      rm_clash(srcTarget, jsSources)
    }
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
).jsSettings(
  excludeDependencies += ("eu.unicredit" %% "akkaactorjsirpatches"),
  compile in Compile <<= (compile in Compile) dependsOn assembleAkkaLibrary,
  publishLocal <<= publishLocal dependsOn assembleAkkaLibrary
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
  ).settings (commonSettings : _*
).enablePlugins (ScalaJSPlugin)



lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(akkaJsActorIrPatches, akkaJsActorJS, akkaTestkitJS, akkaActorTestJS, akkaJsActorStreamJS)
