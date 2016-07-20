val akkaJsVersion = "0.1.4-SNAPSHOT"
val akkaOriginalVersion = "v2.4.8"
val akkaJsActorsProject = "akka-js-actor"

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

def assembleAkkaLibrary(sourceDirs: List[String],
                       akkaProject: String,
                       baseDir: File,
                       targetDir:File,
                       stream: sbt.Keys.TaskStreams) =  {
  import org.eclipse.jgit.api._

  val log = stream.log

  val trgDir = targetDir / "akkaSources" / akkaOriginalVersion

  val currentProjectDirParts = baseDir.toString.split("/").init.toList

  val currentProjectDir = file(currentProjectDirParts.mkString("/"))

  val akkaRepositoryProp = Option(System.getProperty("akka.local.repo"))

  val akkaRepoDir =  akkaRepositoryProp match {
    case None => currentProjectDir / ".." /  akkaJsActorsProject / "js" / "target"/ "akkaSources" / akkaOriginalVersion
    case Some(dir) => file(dir) / akkaOriginalVersion
  }

  val currentProject = currentProjectDirParts.last

  log.info(s"Running assembleAkkaLibrary for $currentProject ")

  log.debug(s"baseDir=$baseDir\ncurrentProjectDir=$currentProjectDir\nsourceDirs=$sourceDirs")

  log.debug(s"trgDir=$trgDir\nakkaRepoDir=$akkaRepoDir\nakkaJsActorsProject=$akkaJsActorsProject\ncurrentProject=$currentProject\nakkaProject=$akkaProject")

  if(!akkaRepoDir.exists) {
    log.info(s"Fetching Akka source version $akkaOriginalVersion into $akkaRepoDir ")

    // Make parent dirs and stuff
    IO.createDirectory(akkaRepoDir)

    // Clone akka source code
    new CloneCommand()
      .setDirectory(akkaRepoDir)
      .setURI("https://github.com/akka/akka.git")
      .call()

    // Checkout proper ref. We do this anyway so we fail if
    // something is wrong
    val git = Git.open(akkaRepoDir)
    log.info(s"Checking out Akka source version $akkaOriginalVersion")
    git.checkout().setName(s"$akkaOriginalVersion").call()
  }

  if (!trgDir.exists) {
    if (currentProject == akkaJsActorsProject) {
      // If the rep dir was overriden copy just the files for this akka project to target
      if (trgDir != akkaRepoDir) {
        IO.createDirectory(trgDir)
        log.info(s"Copying Akka sources to $akkaProject sub-project")
        IO.copyDirectory(
          akkaRepoDir / akkaProject,
          trgDir / akkaProject, overwrite = true)
      }
    } else {
      log.info(s"Copying Akka sources to $akkaProject sub-project")
      // Make parent dirs and stuff
      IO.createDirectory(trgDir / akkaProject)

      IO.copyDirectory(
        akkaRepoDir / akkaProject,
        trgDir / akkaProject, overwrite = true)
    }


    sourceDirs.foreach { part =>
      val partDir = trgDir / akkaProject / "src" / part
      if (partDir.exists) {
        val srcSource = partDir / "scala"
        val srcTarget = currentProjectDir / "shared" / "src" / part / "scala"
        log.info(s"Copying $srcSource to  $srcTarget")
        IO.delete(srcTarget)
        IO.copyDirectory(
          srcSource,
          srcTarget, overwrite = true)

        val boilerplateSource = partDir / "boilerplate"
        if (boilerplateSource.exists()) {
          val boilerplateTarget = currentProjectDir / "js" / "src" / part / "boilerplate"
          log.info(s"Copying $boilerplateSource to  $boilerplateTarget")

          IO.delete(boilerplateTarget)
          IO.copyDirectory(
            boilerplateSource,
            boilerplateTarget, overwrite = true)
        }
        val jsSources = currentProjectDir / "js" / "src" / part / "scala"
        log.info(s"Resolving clashes between $srcTarget to  $jsSources")
        rm_clash(srcTarget, jsSources)
      }

    }
  }

}

lazy val akkaJsActor = crossProject.in(file("akka-js-actor"))
  .settings(commonSettings : _*)
  .settings(
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    assembleAkkaLibrary := {
      assembleAkkaLibrary(List("main"), "akka-actor", baseDirectory.value, target.value, streams.value )
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
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    assembleAkkaLibrary := {
      assembleAkkaLibrary(List("main","test"), "akka-testkit", baseDirectory.value, target.value, streams.value )
    }

  ).jsSettings(
  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0-M16-SNAP6" withSources (),
  libraryDependencies += "org.scala-js" %% "scalajs-test-interface" % "0.6.10-SNAPSHOT" % "test",
  compile in Compile <<= (compile in Compile) dependsOn assembleAkkaLibrary,
  test in Test <<= (test in Test) dependsOn assembleAkkaLibrary,
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
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    assembleAkkaLibrary := {
      assembleAkkaLibrary(List("main","test"), "akka-actor-tests", baseDirectory.value, target.value, streams.value )
    }

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
 ).jsSettings(
  excludeDependencies += ("eu.unicredit" %% "akkaactorjsirpatches"),
  compile in Compile <<= (compile in Compile) dependsOn assembleAkkaLibrary,
  publishLocal <<= publishLocal dependsOn assembleAkkaLibrary
).dependsOn(akkaTestkit % "test->test")

lazy val akkaActorTestJS = akkaActorTest.js

lazy val akkaJsActorStream = crossProject.in(file("akka-js-actor-stream"))
  .settings(commonSettings : _*)
  .settings(
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    assembleAkkaLibrary := {
      assembleAkkaLibrary(List("main"), "akka-stream", baseDirectory.value, target.value, streams.value )
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

lazy val akkaStreamTestkit = crossProject.in(file("akka-js-stream-testkit"))
  .settings(commonSettings: _*)
  .settings(
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    assembleAkkaLibrary := {
      assembleAkkaLibrary(List("main","test"), "akka-stream-testkit", baseDirectory.value, target.value, streams.value )
    }
  ).jsSettings(
  libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.0-M16-SNAP6" withSources (),
  libraryDependencies += "org.scala-js" %% "scalajs-test-interface" % "0.6.10-SNAPSHOT" % "test",
  compile in Compile <<= (compile in Compile) dependsOn assembleAkkaLibrary,
  test in Test <<= (test in Test) dependsOn assembleAkkaLibrary,
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
    // parallelExecution in Test := false,
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    assembleAkkaLibrary := {
      assembleAkkaLibrary(List("main","test"), "akka-stream-tests", baseDirectory.value, target.value, streams.value )
    }
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
 ).jsSettings(
      excludeDependencies += ("eu.unicredit" %% "akkaactorjsirpatches"),
      compile in Compile <<= (compile in Compile) dependsOn assembleAkkaLibrary,
      publishLocal <<= publishLocal dependsOn assembleAkkaLibrary

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
