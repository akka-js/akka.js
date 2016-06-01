This repository is an ongoing effort to port Akka to the JavaScript runtime, thanks to [Scala.js](http://scala-js.org)

## Build it and try the examples

You can start from the published SNAPSHOT:
```scala
libraryDependencies += "akka.js" %%% "akkaactor" % "0.1.1-SNAPSHOT"
```

or you can compile and publish locally yourself:
``` 
$ git clone https://github.com/unicredit/akka.js
$ cd akka.js
$ git checkout refactoring
$ git submodule init
$ git submodule update
$ sbt akkaJsActorIrPatches/compile
$ sbt akkaActorJS/publishLocal
```

Now providing a proper configuration to the ActorSystem you can directly use akka within your scala-js projects.

Then download the examples and follow the README.md available [here](https://github.com/unicredit/akka.js-examples)

## Design documentation

The BSc thesis detailing most of the work and the approach taken can be found [here](pdf/thesis.pdf)

The original codebase derives from Sébastien Doeraene's `scala-js-actors`, you can find his original report [here](http://lampwww.epfl.ch/~doeraene/scalajs-actors-design.pdf).

## Akka version

As of now, code is taken from Akka MASTER

## License

Akka.js is distributed under the
[Scala License](http://www.scala-lang.org/license.html).
