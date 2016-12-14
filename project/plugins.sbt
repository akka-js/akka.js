val scalaJsVersion = "0.6.13"

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.3" exclude ("com.trueaccord.scalapb", "protoc-bridge_2.10"))

addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJsVersion)

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

addSbtPlugin("io.spray" % "sbt-boilerplate" % "0.6.0")

addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.1.0")

libraryDependencies ++= Seq(
  "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "3.2.0.201312181205-r",
  "org.scala-js" %% "scalajs-tools" % scalaJsVersion,
  "org.scala-js" %% "scalajs-ir" % scalaJsVersion,
  //"com.trueaccord.scalapb" %% "compilerplugin-shaded" % "0.5.45-p3"
  "com.trueaccord.scalapb" %% "compilerplugin-shaded" % "0.5.46-SNAPSHOT"
)
