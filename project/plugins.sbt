addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.10")

addSbtPlugin("eu.unicredit" % "sbt-scalajs-ir-patch-plugin" % "0.0.1-SNAPSHOT")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

addSbtPlugin("io.spray" % "sbt-boilerplate" % "0.6.0")

logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Sonatype-Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
