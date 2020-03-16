val akkaJsVersion = "2.2.6.3"
val akkaOriginalVersion = "v2.6.3"

val commonSettings = Seq(
    scalaVersion := "2.13.1",
    crossScalaVersions  := Seq("2.12.10", "2.13.1"),
    organization := "org.akka-js",
    scalacOptions ++= Seq(
        "-deprecation",
        "-unchecked",
        "-feature",
        "-language:postfixOps",
        "-language:reflectiveCalls",
        "-encoding", "utf8"
    ),
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases")
    ),
    parallelExecution in Global := false,
    sources in doc in Compile := Nil,
    scalaJSStage in Global := FastOptStage,
    cancelable in Global := true
)

val publishSettings = Seq(
  publishMavenStyle := true,
  pomIncludeRepository := { x => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
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
        <connection>scm:git:github.com/akka-js/akka.js</connection>
        <developerConnection>scm:git:git@github.com:akka-js/akka.js</developerConnection>
        <url>github.com/akka-js/akka.js</url>
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

import java.io.File

lazy val akkaVersion = settingKey[String]("akkaVersion")
lazy val akkaTargetDir = settingKey[File]("akkaTargetDir")

lazy val assembleAkkaLibrary = taskKey[Unit](
  "Checks out akka standard library from submodules/akka and then applies overrides.")

lazy val fixResources = taskKey[Unit](
  "Fix application.conf presence on first clean build.")

//basically eviction rules
def rm_clash(base: File, target: File): Unit = {
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

def getAkkaSources(targetDir: File, version: String) = {
  import org.eclipse.jgit.api._

  if (!targetDir.exists) {
    //s.log.info(s"Fetching Akka source version ${akkaVersion.value}")

    // Make parent dirs and stuff
    IO.createDirectory(targetDir)

    // Clone akka source code
    // retries are in place to be more travis friendly
    var cloneRetries = 5
    while (cloneRetries > 0) {
     try {
       new CloneCommand()
         .setDirectory(targetDir)
         .setURI("https://github.com/akka/akka.git")
         .call()
       cloneRetries = -1
     } catch {
       case _ : Throwable =>
         cloneRetries -= 1
      }
    }

    val git = Git.open(targetDir)
    //s.log.info(s"Checking out Akka source version ${version}")
    git.checkout().setName(s"${version}").call()
  }
}

def copyToSourceFolder(sourceDir: File, targetDir: File, deleteOld: Boolean = true) = {
  if (deleteOld)
    IO.delete(targetDir)

  IO.copyDirectory(
    sourceDir,
    targetDir,
    overwrite = true,
    preserveLastModified = true)
  (targetDir / ".gitkeep").createNewFile
}

lazy val akkaJsUnsafe = project.in(file("akka-js-unsafe"))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
    )
  )

def fixAwaitImport(folders: Seq[File]) = {
  import scala.sys.process._
  val coursierBin = file("target") / "coursier"
  val scalafixBin = file("target") / "scalafix"

  val scalafixRule = file(".") / "plugins" / "ChangeAwaitImport.scala"

  if (!scalafixBin.exists) {
    // Install scalafix command line
    coursierBin.delete()

    assert { s"curl -Lo ${coursierBin.getAbsolutePath} https://git.io/coursier-cli".! == 0 }
    assert { s"chmod +x ${coursierBin.getAbsolutePath}".! == 0 }
    assert { s"${coursierBin.getAbsolutePath} bootstrap ch.epfl.scala:scalafix-cli_2.12.10:0.9.11 -f --main scalafix.cli.Cli -o ${scalafixBin.getAbsolutePath}".! == 0 }
  }

  def fixCommand(targetFolder: String) =
    s"${scalafixBin.getAbsolutePath} -r file:${scalafixRule} --files ${targetFolder}"
  
  folders.foreach { folder =>
    if (folder.exists) {
      fixCommand(folder.getAbsolutePath).!
    }
  }
}

import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

lazy val akkaJsActor = crossProject(JSPlatform)
  .crossType(CrossType.Full)
  .in(file("akka-js-actor"))
  .settings(commonSettings : _*)
  .settings(
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    akkaTargetDir := target.value / "akkaSources" / akkaVersion.value,
    assembleAkkaLibrary := {
      getAkkaSources(akkaTargetDir.value, akkaVersion.value)
      val srcTarget = file("akka-js-actor/shared/src/main/scala")
      copyToSourceFolder(
        akkaTargetDir.value / "akka-actor" / "src" / "main" / "scala",
        srcTarget
      )

      copyToSourceFolder(
        akkaTargetDir.value / "akka-actor" / "src" / "main" / "scala-2.12",
        file("akka-js-actor/shared/src/main/scala-2.12")
      )
      copyToSourceFolder(
        akkaTargetDir.value / "akka-actor" / "src" / "main" / "scala-2.13-",
        file("akka-js-actor/shared/src/main/scala-2.12"),
        false
      )
      copyToSourceFolder(
        akkaTargetDir.value / "akka-actor" / "src" / "main" / "scala-2.13",
        file("akka-js-actor/shared/src/main/scala-2.13")
      )
      copyToSourceFolder(
        akkaTargetDir.value / "akka-actor" / "src" / "main" / "scala-2.13+",
        file("akka-js-actor/shared/src/main/scala-2.13"),
        false
      )

      copyToSourceFolder(
        akkaTargetDir.value / "akka-actor" / "src" / "main" / "boilerplate",
        file("akka-js-actor/js/src/main/boilerplate")
      )

      val jsSources = file("akka-js-actor/js/src/main/scala")

      rm_clash(srcTarget, jsSources)
    },
    fixResources := {
      val compileConf = (resourceDirectory in Compile).value / "application.conf"
      if (compileConf.exists)
        IO.copyFile(
          compileConf,
          (classDirectory in Compile).value / "application.conf"
        )
      val testConf = (resourceDirectory in Test).value / "application.conf"
      if (testConf.exists) {
        IO.copyFile(
          testConf,
          (classDirectory in Test).value / "application.conf"
        )
      }
    }
   ).jsSettings(
    scalaJSOptimizerOptions ~= { _.withCheckScalaJSIR(true) },
    libraryDependencies ++= {
      Seq(
        "org.akka-js" %%% "shocon" % "0.5.0",
        "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0"
      )
    },
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
    ),
    compile in Compile := {
      val analysis = (compile in Compile).value
      val classDir = (classDirectory in Compile).value
      val hackDirs = (products in (akkaJsActorIrPatches, Compile)).value

      for (hackDir <- hackDirs)
        org.akkajs.IrPatcherPlugin.hackAllUnder(classDir, hackDir)

      analysis
    }
  ).jsSettings(
    useAnnotationAdderPluginSettings : _*
  ).jsSettings(
    publishSettings : _*
  ).jsSettings(
    excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
    compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary, fixResources).value},
    publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary, fixResources).value},
    PgpKeys.publishSigned := {PgpKeys.publishSigned.dependsOn(assembleAkkaLibrary, fixResources).value}
  ).enablePlugins(spray.boilerplate.BoilerplatePlugin)

lazy val akkaJsActorJS = akkaJsActor.js.dependsOn(
  akkaJsUnsafe % "provided"
)

lazy val akkaJsTestkit = crossProject(JSPlatform)
  .crossType(CrossType.Full)
  .in(file("akka-js-testkit"))
  .settings(commonSettings: _*)
  .settings(
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    akkaTargetDir := file("akka-js-actor/js/target/") / "akkaSources" / akkaVersion.value,
    assembleAkkaLibrary := {
      getAkkaSources(akkaTargetDir.value, akkaVersion.value)
      val srcTarget = file("akka-js-testkit/shared/src/main/scala")
      copyToSourceFolder(
        akkaTargetDir.value / "akka-testkit" / "src" / "main" / "scala",
        srcTarget
      )

      val jsSources = file("akka-js-testkit/js/src/main/scala")

      rm_clash(srcTarget, jsSources)

      val testTarget = file("akka-js-testkit/shared/src/test/scala")
      copyToSourceFolder(
        akkaTargetDir.value / "akka-testkit" / "src" / "test" / "scala",
        testTarget
      )

      val jsTestSources = file("akka-js-testkit/js/src/test/scala")

      rm_clash(testTarget, jsTestSources)

      fixAwaitImport(Seq(baseDirectory.value / ".."))
    },
    fixResources := {
      val compileConf = (resourceDirectory in Compile).value / "application.conf"
      if (compileConf.exists)
        IO.copyFile(
          compileConf,
          (classDirectory in Compile).value / "application.conf"
        )
      val testConf = (resourceDirectory in Test).value / "application.conf"
      if (testConf.exists) {
        IO.copyFile(
          testConf,
          (classDirectory in Test).value / "application.conf"
        )
      }
    }
  ).jsSettings(publishSettings : _*)
  .jsSettings(useAnnotationAdderPluginSettings : _*)
  .jsSettings(
    scalaJSOptimizerOptions ~= { _.withCheckScalaJSIR(true) },
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.1.0" withSources ()
    ),
    scalaJSStage in Global := FastOptStage,
    publishArtifact in (Test, packageBin) := true,
    //preLinkJSEnv := jsEnv.value,
    //postLinkJSEnv := jsEnv.value.withSourceMap(true)
    excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
    compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary, fixResources).value},
    publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary, fixResources).value},
    PgpKeys.publishSigned := {PgpKeys.publishSigned.dependsOn(assembleAkkaLibrary, fixResources).value}
  ).dependsOn(akkaJsActor)

lazy val akkaJsTestkitJS = akkaJsTestkit.js.dependsOn(akkaJsActorJS)

lazy val akkaActorTest = crossProject(JSPlatform)
  .crossType(CrossType.Full)
  .in(file("akka-js-actor-tests"))
  .settings(commonSettings: _*)
  .settings(
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    akkaTargetDir := file("akka-js-actor/js/target/") / "akkaSources" / akkaVersion.value,
    assembleAkkaLibrary := {
      getAkkaSources(akkaTargetDir.value, akkaVersion.value)
      val srcTarget = file("akka-js-actor-tests/shared/src/test/scala")
      copyToSourceFolder(
        akkaTargetDir.value / "akka-actor-tests" / "src" / "test" / "scala",
        srcTarget
      )

      val jsSources = file("akka-js-actor-tests/js/src/test/scala")

      rm_clash(srcTarget, jsSources)

      fixAwaitImport(Seq(baseDirectory.value / ".."))
    }
  ).jsSettings(
    scalaJSStage in Global := FastOptStage,
    publishArtifact in (Test, packageBin) := true,
    //scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) },
    //preLinkJSEnv := jsEnv.value,
    //postLinkJSEnv := jsEnv.value.withSourceMap(true),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % "1.14.2" % "test"
    ),
    excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
    compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary).value},
    publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary).value}
 ).dependsOn(akkaJsTestkit % "test->test")

lazy val akkaActorTestJS = akkaActorTest.js

lazy val akkaJsActorStream = crossProject(JSPlatform)
  .crossType(CrossType.Full)
  .in(file("akka-js-actor-stream"))
  .settings(commonSettings : _*)
  .settings(
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    akkaTargetDir := file("akka-js-actor/js/target/") / "akkaSources" / akkaVersion.value,
    assembleAkkaLibrary := {
      getAkkaSources(akkaTargetDir.value, akkaVersion.value)
      val srcTarget = file("akka-js-actor-stream/shared/src/main/scala")
      copyToSourceFolder(
        akkaTargetDir.value / "akka-stream" / "src" / "main" / "scala",
        srcTarget
      )
      copyToSourceFolder(
        akkaTargetDir.value / "akka-stream" / "src" / "main" / "boilerplate",
        file("akka-js-actor-stream/js/src/main/boilerplate")
      )

      val jsSources = file("akka-js-actor-stream/js/src/main/scala")

      rm_clash(srcTarget, jsSources)
    },
    fixResources := {
      val compileConf = (resourceDirectory in Compile).value / "application.conf"
      if (compileConf.exists)
        IO.copyFile(
          compileConf,
          (classDirectory in Compile).value / "application.conf"
        )
      val testConf = (resourceDirectory in Test).value / "application.conf"
      if (testConf.exists) {
        IO.copyFile(
          testConf,
          (classDirectory in Test).value / "application.conf"
        )
      }
    }
  ).jsSettings(
    useAnnotationAdderPluginSettings : _*
  ).jsSettings(
    publishSettings : _*
  ).jsSettings(
    scalaJSOptimizerOptions ~= { _.withCheckScalaJSIR(true) },
    excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
    compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary, fixResources).value},
    publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary, fixResources).value},
    PgpKeys.publishSigned := {PgpKeys.publishSigned.dependsOn(assembleAkkaLibrary, fixResources).value}
  ).enablePlugins(spray.boilerplate.BoilerplatePlugin).dependsOn(akkaJsActor)

lazy val akkaJsActorStreamJS = akkaJsActorStream.js

lazy val akkaJsStreamTestkit = crossProject(JSPlatform)
  .crossType(CrossType.Full)
  .in(file("akka-js-stream-testkit"))
  .settings(commonSettings: _*)
  .jsSettings(publishSettings : _*)
  .settings(
    // parallelExecution in Test := false,
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    akkaTargetDir := file("akka-js-actor/js/target/") / "akkaSources" / akkaVersion.value,
    assembleAkkaLibrary := {
      getAkkaSources(akkaTargetDir.value, akkaVersion.value)
      val srcTestTarget = file("akka-js-stream-testkit/shared/src/test/scala")
      val srcMainTarget = file("akka-js-stream-testkit/shared/src/main/scala")
      copyToSourceFolder(
        akkaTargetDir.value / "akka-stream-testkit" / "src" / "test" / "scala",
        srcTestTarget
      )
      copyToSourceFolder(
        akkaTargetDir.value / "akka-stream-testkit" / "src" / "main" / "scala",
        srcMainTarget
      )

      val jsTestSources = file("akka-js-stream-testkit/js/src/test/scala")
      val jsMainSources = file("akka-js-stream-testkit/js/src/main/scala")

      rm_clash(srcTestTarget, jsTestSources)
      rm_clash(srcMainTarget, jsMainSources)

      fixAwaitImport(Seq(baseDirectory.value / ".."))
    }
  ).jsSettings(
    scalaJSOptimizerOptions ~= { _.withCheckScalaJSIR(true) },
    scalaJSStage in Global := FastOptStage,
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.1.0" withSources ()
    ),
    publishArtifact in (Test, packageBin) := true,
    //scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) },
    //preLinkJSEnv := jsEnv.value,
    //postLinkJSEnv := jsEnv.value.withSourceMap(true),
    excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
    compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary).value},
    publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary).value},
    PgpKeys.publishSigned := {PgpKeys.publishSigned.dependsOn(assembleAkkaLibrary).value}
 ).dependsOn(akkaJsActorStream, akkaJsTestkit % "*->*")

 lazy val akkaJsStreamTestkitJS = akkaJsStreamTestkit.js

 lazy val akkaStreamTest = crossProject(JSPlatform)
  .crossType(CrossType.Full)
  .in(file("akka-js-stream-tests"))
   .settings(commonSettings: _*)
   .settings(
     version := akkaJsVersion,
     akkaVersion := akkaOriginalVersion,
     akkaTargetDir := file("akka-js-actor/js/target/") / "akkaSources" / akkaVersion.value,
     assembleAkkaLibrary := {
       getAkkaSources(akkaTargetDir.value, akkaVersion.value)
       val srcTarget = file("akka-js-stream-tests/shared/src/test/scala")
       copyToSourceFolder(
         akkaTargetDir.value / "akka-stream-tests" / "src" / "test" / "scala",
         srcTarget
       )

       val jsSources = file("akka-js-stream-tests/js/src/test/scala")

       rm_clash(srcTarget, jsSources)

       fixAwaitImport(Seq(baseDirectory.value / ".."))
     }
   ).jsSettings(
     libraryDependencies ++= Seq(
       "org.scalacheck" %%% "scalacheck" % "1.14.2" % "test"
     ),
     scalaJSStage in Global := FastOptStage,
     publishArtifact in (Test, packageBin) := true
     //scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) },
     //preLinkJSEnv := jsEnv.value,
     //postLinkJSEnv := jsEnv.value.withSourceMap(true)
  ).jsSettings(
       excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
       compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary).value},
       publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary).value}
  ).dependsOn(akkaJsStreamTestkit % "test->test", akkaJsActorStream)

  lazy val akkaStreamTestJS = akkaStreamTest.js

  lazy val akkaJsActorTyped = crossProject(JSPlatform)
  .crossType(CrossType.Full)
  .in(file("akka-js-actor-typed"))
    .settings(commonSettings : _*)
    .settings(
      version := akkaJsVersion,
      akkaVersion := akkaOriginalVersion,
      akkaTargetDir := file("akka-js-actor/js/target/") / "akkaSources" / akkaVersion.value,
      assembleAkkaLibrary := {
        getAkkaSources(akkaTargetDir.value, akkaVersion.value)
        val srcTarget = file("akka-js-actor-typed/shared/src/main/scala")
        copyToSourceFolder(
          akkaTargetDir.value / "akka-actor-typed" / "src" / "main" / "scala",
          srcTarget
        )

        val jsSources = file("akka-js-actor-typed/js/src/main/scala")

        rm_clash(srcTarget, jsSources)
      },
      fixResources := {
        val compileConf = (resourceDirectory in Compile).value / "application.conf"
        if (compileConf.exists)
          IO.copyFile(
            compileConf,
            (classDirectory in Compile).value / "application.conf"
          )
        val testConf = (resourceDirectory in Test).value / "application.conf"
        if (testConf.exists) {
          IO.copyFile(
            testConf,
            (classDirectory in Test).value / "application.conf"
          )
        }
      }
    ).jsSettings(
      useAnnotationAdderPluginSettings : _*
    ).jsSettings(
      publishSettings : _*
    ).jsSettings(
      scalaJSOptimizerOptions ~= { _.withCheckScalaJSIR(true) },
      libraryDependencies ++= Seq(
        "org.wvlet.airframe" %%% "airframe-log" % "19.12.4",
        "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0"
      ),
      excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
      compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary, fixResources).value},
      publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary, fixResources).value},
      PgpKeys.publishSigned := {PgpKeys.publishSigned.dependsOn(assembleAkkaLibrary, fixResources).value}
    ).enablePlugins(spray.boilerplate.BoilerplatePlugin).dependsOn(akkaJsActor)

  lazy val akkaJsActorTypedJS = akkaJsActorTyped.js.dependsOn(
    akkaJsUnsafe % "provided"
  )

  lazy val akkaJsTypedTestkit = crossProject(JSPlatform)
  .crossType(CrossType.Full)
  .in(file("akka-js-typed-testkit"))
    .settings(commonSettings: _*)
    .jsSettings(publishSettings : _*)
    .settings(
      // parallelExecution in Test := false,
      version := akkaJsVersion,
      akkaVersion := akkaOriginalVersion,
      akkaTargetDir := file("akka-js-actor/js/target/") / "akkaSources" / akkaVersion.value,
      assembleAkkaLibrary := {
        getAkkaSources(akkaTargetDir.value, akkaVersion.value)
        val srcTarget = file("akka-js-typed-testkit/shared/src/test/scala")
        copyToSourceFolder(
          akkaTargetDir.value / "akka-actor-testkit-typed" / "src" / "test" / "scala",
          srcTarget
        )
        copyToSourceFolder(
          akkaTargetDir.value / "akka-actor-testkit-typed" / "src" / "main" / "scala",
          file("akka-js-typed-testkit/shared/src/main/scala")
        )

        rm_clash(
          file("akka-js-typed-testkit/shared/src/main/scala"),
          file("akka-js-typed-testkit/js/src/main/scala")
        )

        rm_clash(
          file("akka-js-typed-testkit/shared/src/test/scala"),
          file("akka-js-typed-testkit/js/src/test/scala")
        )

        fixAwaitImport(Seq(baseDirectory.value / ".."))
      }
    ).jsSettings(
      scalaJSOptimizerOptions ~= { _.withCheckScalaJSIR(true) },
      scalaJSStage in Global := FastOptStage,
      publishArtifact in (Test, packageBin) := true,
      libraryDependencies ++= Seq(
        "org.scalatest" %%% "scalatest" % "3.1.0" withSources ()) ++
        (CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, minor)) if minor < 13 => Seq("org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0" % "provided")
          case _                              => Seq()
        }),
      //scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) },
      //preLinkJSEnv := jsEnv.value,
      //postLinkJSEnv := jsEnv.value.withSourceMap(true),
      excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
      compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary).value},
      publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary).value},
      PgpKeys.publishSigned := {PgpKeys.publishSigned.dependsOn(assembleAkkaLibrary).value}
   ).dependsOn(akkaJsActorTyped, akkaJsTestkit % "*->*")

   lazy val akkaJsTypedTestkitJS = akkaJsTypedTestkit.js

   lazy val akkaTypedTest = crossProject(JSPlatform)
  .crossType(CrossType.Full)
  .in(file("akka-js-typed-tests"))
     .settings(commonSettings: _*)
     .settings(
       version := akkaJsVersion,
       akkaVersion := akkaOriginalVersion,
       akkaTargetDir := file("akka-js-actor/js/target/") / "akkaSources" / akkaVersion.value,
       assembleAkkaLibrary := {
         getAkkaSources(akkaTargetDir.value, akkaVersion.value)
         val srcTarget = file("akka-js-typed-tests/shared/src/test/scala")
         copyToSourceFolder(
           akkaTargetDir.value / "akka-actor-typed-tests" / "src" / "test" / "scala",
           srcTarget
         )

         val jsSources = file("akka-js-typed-tests/js/src/test/scala")

         rm_clash(srcTarget, jsSources)

         fixAwaitImport(Seq(baseDirectory.value / ".."))
       }
     ).jsSettings(
       libraryDependencies ++= Seq(
         "org.scalacheck" %%% "scalacheck" % "1.14.2" % "test"
       ),
       scalaJSStage in Global := FastOptStage,
       publishArtifact in (Test, packageBin) := true
       //scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) },
       //preLinkJSEnv := jsEnv.value,
       //postLinkJSEnv := jsEnv.value.withSourceMap(true)
    ).jsSettings(
         excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
         compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary).value},
         publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary).value}
    ).dependsOn(akkaJsTypedTestkit % "test->test", akkaJsActorStream)

    lazy val akkaTypedTestJS = akkaTypedTest.js

  lazy val akkaJsActorStreamTyped = crossProject(JSPlatform)
  .crossType(CrossType.Full)
  .in(file("akka-js-stream-typed"))
    .settings(commonSettings : _*)
    .settings(
      version := akkaJsVersion,
      akkaVersion := akkaOriginalVersion,
      akkaTargetDir := file("akka-js-actor/js/target/") / "akkaSources" / akkaVersion.value,
      assembleAkkaLibrary := {
        getAkkaSources(akkaTargetDir.value, akkaVersion.value)
        val srcTarget = file("akka-js-stream-typed/shared/src/main/scala")
        copyToSourceFolder(
          akkaTargetDir.value / "akka-stream-typed" / "src" / "main" / "scala",
          srcTarget
        )
        val testTarget = file("akka-js-stream-typed/shared/src/test/scala")
        copyToSourceFolder(
          akkaTargetDir.value / "akka-stream-typed" / "src" / "test" / "scala",
          testTarget
        )

        val jsSources = file("akka-js-stream-typed/js/src/main/scala")

        rm_clash(srcTarget, jsSources)

        val jsTests = file("akka-js-stream-typed/js/src/test/scala")

        rm_clash(testTarget, jsTests)

        fixAwaitImport(Seq(baseDirectory.value / ".."))
      },
      fixResources := {
        val compileConf = (resourceDirectory in Compile).value / "application.conf"
        if (compileConf.exists)
          IO.copyFile(
            compileConf,
            (classDirectory in Compile).value / "application.conf"
          )
        val testConf = (resourceDirectory in Test).value / "application.conf"
        if (testConf.exists) {
          IO.copyFile(
            testConf,
            (classDirectory in Test).value / "application.conf"
          )
        }
      }
    ).jsSettings(
      useAnnotationAdderPluginSettings : _*
    ).jsSettings(
      publishSettings : _*
    ).jsSettings(
      scalaJSStage in Global := FastOptStage,
      publishArtifact in (Test, packageBin) := true,
      scalaJSOptimizerOptions ~= { _.withCheckScalaJSIR(true) },
      excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
      compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary, fixResources).value},
      publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary, fixResources).value}
    ).dependsOn(
      akkaJsActorStream,
      akkaJsActorTyped,
      akkaJsTypedTestkit % "test->test",
      akkaTypedTest % "test->test", // to report upstream
      akkaJsStreamTestkit % "test->test"
    )

    lazy val akkaJsActorStreamTypedJS = akkaJsActorStreamTyped.js


//COMPILER PLUGINS SECTION

//add scala.js annotations to proper classes
lazy val annotationAdderPlugin = Project(
    id   = "annotationAdderPlugin",
    base = file("plugins/annotation-adder-plugin")
  ) settings (
    libraryDependencies += ("org.scala-lang" % "scala-compiler" % scalaVersion.value),
    publishArtifact in Compile := false
  ) settings (commonSettings : _*)

lazy val useAnnotationAdderPluginSettings = Seq(
    scalacOptions in Compile += (
      "-Xplugin:" + (Keys.`package` in (annotationAdderPlugin, Compile)).value.getAbsolutePath.toString
    )
  )

//SCALAJS IR PATCHER SECTION

//core patches project
lazy val akkaJsActorIrPatches = Project(
    id   = "akkaActorJSIrPatches",
    base = file("akka-js-actor-ir-patches")
   ).
   settings (
    publishArtifact in Compile := true
  ).settings (commonSettings : _*
  ).enablePlugins (ScalaJSPlugin)



lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(
    akkaJsActorIrPatches,
    akkaJsActorJS,
    akkaJsTestkitJS,
    akkaActorTestJS,
    akkaJsActorStreamJS,
    akkaJsStreamTestkitJS,
    akkaStreamTestJS,
    akkaJsActorTypedJS,
    akkaJsTypedTestkitJS,
    akkaTypedTestJS,
    akkaJsActorStreamTypedJS
  )
