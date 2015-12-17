addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.6-SNAPSHOT")

addSbtPlugin("eu.unicredit" % "sbt-scalajs-ir-patch-plugin" % "0.0.1-SNAPSHOT")

logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Sonatype-Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
