name := """chat-full-stack-new"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
/*  jdbc,
  anorm,
  cache,*/
  ws,
  "org.webjars" %% "webjars-play" % "2.3.0-2",
  "org.webjars" % "jquery" % "2.1.3"
)
