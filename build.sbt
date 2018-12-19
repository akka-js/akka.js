val akkaJsVersion = "1.2.5.19"
val akkaOriginalVersion = "v2.5.19"

val commonSettings = Seq(
    scalaVersion := "2.12.6",
    crossScalaVersions  := Seq("2.12.6", "2.11.12"),
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
      Resolver.typesafeRepo("releases"),
      Resolver.sonatypeRepo("snapshots")
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

def copyToSourceFolder(sourceDir: File, targetDir: File) = {
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

lazy val akkaJsActor = crossProject.in(file("akka-js-actor"))
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
    libraryDependencies ++= Seq(
      "org.akka-js" %%% "shocon" % "0.4.1",
      "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0" % "provided"
    ),
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

lazy val akkaJsTestkit = crossProject.in(file("akka-js-testkit"))
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
      "org.scalatest" %%% "scalatest" % "3.0.4" withSources ()
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

lazy val akkaActorTest = crossProject.in(file("akka-js-actor-tests"))
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
    }
  ).jsSettings(
    scalaJSStage in Global := FastOptStage,
    publishArtifact in (Test, packageBin) := true,
    //scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) },
    //preLinkJSEnv := jsEnv.value,
    //postLinkJSEnv := jsEnv.value.withSourceMap(true),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % "1.13.4" % "test"
    ),
    excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
    compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary).value},
    publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary).value}
 ).dependsOn(akkaJsTestkit % "test->test")

lazy val akkaActorTestJS = akkaActorTest.js

lazy val akkaJsActorStream = crossProject.in(file("akka-js-actor-stream"))
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
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0" % "provided"
    ),
    excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
    compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary, fixResources).value},
    publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary, fixResources).value},
    PgpKeys.publishSigned := {PgpKeys.publishSigned.dependsOn(assembleAkkaLibrary, fixResources).value}
  ).enablePlugins(spray.boilerplate.BoilerplatePlugin).dependsOn(akkaJsActor)

lazy val akkaJsActorStreamJS = akkaJsActorStream.js

lazy val akkaJsStreamTestkit = crossProject.in(file("akka-js-stream-testkit"))
  .settings(commonSettings: _*)
  .jsSettings(publishSettings : _*)
  .settings(
    // parallelExecution in Test := false,
    version := akkaJsVersion,
    akkaVersion := akkaOriginalVersion,
    akkaTargetDir := file("akka-js-actor/js/target/") / "akkaSources" / akkaVersion.value,
    assembleAkkaLibrary := {
      getAkkaSources(akkaTargetDir.value, akkaVersion.value)
      val srcTarget = file("akka-js-stream-testkit/shared/src/test/scala")
      copyToSourceFolder(
        akkaTargetDir.value / "akka-stream-testkit" / "src" / "test" / "scala",
        srcTarget
      )
      copyToSourceFolder(
        akkaTargetDir.value / "akka-stream-testkit" / "src" / "main" / "scala",
        file("akka-js-stream-testkit/shared/src/main/scala")
      )

      val jsSources = file("akka-js-stream-testkit/js/src/test/scala")

      rm_clash(srcTarget, jsSources)
    }
  ).jsSettings(
    scalaJSOptimizerOptions ~= { _.withCheckScalaJSIR(true) },
    scalaJSStage in Global := FastOptStage,
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

 lazy val akkaStreamTest = crossProject.in(file("akka-js-stream-tests"))
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
     }
   ).jsSettings(
     libraryDependencies ++= Seq(
       "org.scalacheck" %%% "scalacheck" % "1.13.4" % "test"
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

  lazy val akkaJsActorTyped = crossProject.in(file("akka-js-actor-typed"))
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
        "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0" % "provided"
      ),
      excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
      compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary, fixResources).value},
      publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary, fixResources).value},
      PgpKeys.publishSigned := {PgpKeys.publishSigned.dependsOn(assembleAkkaLibrary, fixResources).value}
    ).enablePlugins(spray.boilerplate.BoilerplatePlugin).dependsOn(akkaJsActor)

  lazy val akkaJsActorTypedJS = akkaJsActorTyped.js.dependsOn(
    akkaJsUnsafe % "provided"
  )

  lazy val akkaJsTypedTestkit = crossProject.in(file("akka-js-typed-testkit"))
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
      }
    ).jsSettings(
      scalaJSOptimizerOptions ~= { _.withCheckScalaJSIR(true) },
      scalaJSStage in Global := FastOptStage,
      publishArtifact in (Test, packageBin) := true,
      libraryDependencies ++= Seq(
        "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0" % "provided"
      ),
      //scalaJSOptimizerOptions ~= { _.withDisableOptimizer(true) },
      //preLinkJSEnv := jsEnv.value,
      //postLinkJSEnv := jsEnv.value.withSourceMap(true),
      excludeDependencies += ("org.akka-js" %% "akkaactorjsirpatches"),
      compile in Compile := {(compile in Compile).dependsOn(assembleAkkaLibrary).value},
      publishLocal := {publishLocal.dependsOn(assembleAkkaLibrary).value},
      PgpKeys.publishSigned := {PgpKeys.publishSigned.dependsOn(assembleAkkaLibrary).value}
   ).dependsOn(akkaJsActorTyped, akkaJsTestkit % "*->*")

   lazy val akkaJsTypedTestkitJS = akkaJsTypedTestkit.js

   lazy val akkaTypedTest = crossProject.in(file("akka-js-typed-tests"))
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
       }
     ).jsSettings(
       libraryDependencies ++= Seq(
         "org.scalacheck" %%% "scalacheck" % "1.13.4" % "test"
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
    ).dependsOn(akkaJsTypedTestkit % "test->test", akkaJsActorStream
    ).jsConfigure(
      _.enablePlugins(ScalaJSJUnitPlugin)
    )

    lazy val akkaTypedTestJS = akkaTypedTest.js

  lazy val akkaJsActorStreamTyped = crossProject.in(file("akka-js-stream-typed"))
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
