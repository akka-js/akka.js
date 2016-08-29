addSbtPlugin("com.trueaccord.scalapb" % "sbt-scalapb" % "0.5.34")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.11")

addSbtPlugin("eu.unicredit" % "sbt-scalajs-ir-patch-plugin" % "0.0.1-SNAPSHOT")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

addSbtPlugin("io.spray" % "sbt-boilerplate" % "0.6.0")

addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.1.0")

logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Sonatype-Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.scala-js" %% "scalajs-env-selenium" % "0.1.3",
  "com.github.os72" % "protoc-jar" % "3.0.0-b3",
  "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "3.2.0.201312181205-r"
)
