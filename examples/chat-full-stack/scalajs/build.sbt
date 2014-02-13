import ScalaJSKeys._

name := "Scala.js actors examples - chat client"

normalizedName := "scalajs-actors-example-chat-client"

libraryDependencies +=
  "org.scala-lang.modules.scalajs" %% "scalajs-jquery" % "0.2"

sources in (Compile, packageExportedProductsJS) +=
  baseDirectory.value / "../public/javascripts/startup.js"
