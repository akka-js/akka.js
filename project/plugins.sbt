val scalaJsVersion = "1.0.1"

addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJsVersion)

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")

addSbtPlugin("io.spray" % "sbt-boilerplate" % "0.6.1")

libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "3.2.0.201312181205-r",
  "org.scala-js" %% "scalajs-linker" % scalaJsVersion,
  "org.scala-js" %% "scalajs-linker-interface" % scalaJsVersion,
  "org.scala-js" %% "scalajs-ir" % scalaJsVersion
)

logLevel := Level.Warn
