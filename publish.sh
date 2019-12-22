#! /bin/sh

sbt clean

sbt "akkaJsActorJS/compile"

sbt ";++2.12.10;akkaJsActorJS/publishSigned;akkaJsActorStreamJS/publishSigned;akkaJsActorStreamTypedJS/publishSigned;akkaJsActorTypedJS/publishSigned;akkaJsStreamTestkitJS/publishSigned;akkaJsTestkitJS/publishSigned;akkaJsTypedTestkitJS/publishSigned"

# temporary disable 2.13
# sbt ";++2.13.1;akkaJsActorJS/publishSigned;akkaJsActorStreamJS/publishSigned;akkaJsActorStreamTypedJS/publishSigned;akkaJsActorTypedJS/publishSigned;akkaJsStreamTestkitJS/publishSigned;akkaJsTestkitJS/publishSigned;akkaJsTypedTestkitJS/publishSigned"

sbt sonatypeReleaseAll
