#! /bin/sh

sbt clean

sbt "akkaJsActorJS/compile"

sbt ";++2.11.12;akkaJsActorJS/publishSigned;akkaJsActorStreamJS/publishSigned;akkaJsActorStreamTypedJS/publishSigned;akkaJsActorTypedJS/publishSigned;akkaJsStreamTestkitJS/publishSigned;akkaJsTestkitJS/publishSigned;akkaJsTypedTestkitJS/publishSigned"

sbt ";++2.12.8;akkaJsActorJS/publishSigned;akkaJsActorStreamJS/publishSigned;akkaJsActorStreamTypedJS/publishSigned;akkaJsActorTypedJS/publishSigned;akkaJsStreamTestkitJS/publishSigned;akkaJsTestkitJS/publishSigned;akkaJsTypedTestkitJS/publishSigned"

sbt sonatypeReleaseAll
