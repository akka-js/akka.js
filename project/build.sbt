addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.2")

// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.0-M3")

//to generate eclipse project
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "3.0.0")

//to generate idea project
addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")
