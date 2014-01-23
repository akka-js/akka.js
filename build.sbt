import ScalaJSKeys._

val commonSettings = Seq(
    organization := "org.scalajs",
    version := "0.1-SNAPSHOT",
    normalizedName ~= { _.replace("scala-js", "scalajs") },
    scalacOptions ++= Seq(
        "-deprecation",
        "-unchecked",
        "-feature",
        "-encoding", "utf8"
    )
)

lazy val root = project.in(file(".")).settings(commonSettings: _*)
  .aggregate(actors, akkaWebsocketBridge)

lazy val actors = project.settings(commonSettings: _*)
  .settings(
      unmanagedSourceDirectories in Compile +=
        (sourceDirectory in Compile).value / "wscommon"
  )

lazy val akkaWebsocketBridge = project.in(file("akka-websocket-bridge"))
  .settings(commonSettings: _*)
  .settings(
      unmanagedSourceDirectories in Compile +=
        (sourceDirectory in (actors, Compile)).value / "wscommon"
  )

lazy val examples = project.settings(commonSettings: _*)
  .aggregate(webworkersExample, faultToleranceExample,
      chatExample, chatExampleScalaJS)

lazy val webworkersExample = project.in(file("examples/webworkers"))
  .settings(commonSettings: _*)
  .dependsOn(actors)

lazy val faultToleranceExample = project.in(file("examples/faulttolerance"))
  .settings(commonSettings: _*)
  .dependsOn(actors)

lazy val chatExample = project.in(file("examples/chat-full-stack"))
  .settings(commonSettings: _*)
  .dependsOn(akkaWebsocketBridge)
  .settings(
      unmanagedSourceDirectories in Compile +=
        baseDirectory.value / "cscommon"
  )

lazy val chatExampleScalaJS = project.in(file("examples/chat-full-stack/scalajs"))
  .settings((commonSettings ++ scalaJSSettings): _*)
  .dependsOn(actors)
  .settings(
      unmanagedSourceDirectories in Compile +=
        (baseDirectory in chatExample).value / "cscommon",
      packageJS in Compile <<= (packageJS in Compile) triggeredBy (compile in (chatExample, Compile))
  )
  .settings(
      artifactPath in (Compile, packageExternalDepsJS) :=
        ((baseDirectory in chatExample).value / "public/javascripts" / ((moduleName in (Compile, packageExternalDepsJS)).value + "-extdeps.js")),
      artifactPath in (Compile, packageInternalDepsJS) :=
        ((baseDirectory in chatExample).value / "public/javascripts" / ((moduleName in (Compile, packageInternalDepsJS)).value + "-intdeps.js")),
      artifactPath in (Compile, packageExportedProductsJS) :=
        ((baseDirectory in chatExample).value / "public/javascripts" / ((moduleName in (Compile, packageExportedProductsJS)).value + ".js"))
  )
