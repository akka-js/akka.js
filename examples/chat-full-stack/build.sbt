name := "scalajs-actors-examples-chat"

//libraryDependencies ++= Seq(
//    jdbc,
//    anorm,
//    cache
//)

play.Project.playScalaSettings

libraryDependencies ++= Seq(
    "org.scalajs" %% "scalajs-pickling-play-json" % "0.3-SNAPSHOT"
)
