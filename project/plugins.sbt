addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.10")

addSbtPlugin("eu.unicredit" % "sbt-scalajs-ir-patch-plugin" % "0.0.1-SNAPSHOT")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.2")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")

addSbtPlugin("io.spray" % "sbt-boilerplate" % "0.6.0")

addSbtPlugin("com.eed3si9n" % "sbt-dirty-money" % "0.1.0")

libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit.pgm" % "3.2.0.201312181205-r"

logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Sonatype-Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "org.scala-js" %% "scalajs-env-selenium" % "0.1.3"
