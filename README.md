## Warning: unstable branch, compilation might be broken. If you want a stable version, please check out [master](https://github.com/unicredit/akka.js/tree/master)

<p align="center">
<img width="400" src="logo/akkajs.png">
</p>

This repository contains an ongoing effort to port Akka to the JavaScript runtime, thanks to [Scala.js](http://scala-js.org)

**[Current status](https://github.com/unicredit/akka.js/issues/4)**

## Build it and try the examples

If you are brave and you wanna compile this bleeding edge version you need to:
```
$ git clone https://github.com/sirthias/parboiled2.git
$ cd parboiled2/
$ git checkout release-2.1-js
$ sbt publishLocal  // StackOverflowException if try fastOptJS
$ # the above published to $HOME/.ivy2/local/org.parboiled/parboiled_sjs0.6_2.11/2.1.1-SNAPSHOT/
$ cd ..
 
$ git clone https://github.com/unicredit/scalajs-ir-patcher
$ cd scalajs-ir-patcher
$ sbt publishLocal
$ # publishes to $HOME/.ivy2/local/eu.unicredit/irpatchplugin/scala_2.10/sbt_0.13/0.0.1-SNAPSHOT
$ cd ..
 
$ git clone https://github.com/evacchi/shocon
$ cd shocon
$ sbt publishLocal
$ # publishes to $HOME/.ivy2/local/eu.unicredit/shocon_2.11/0.0.1-SNAPSHOT
$ #              $HOME/.ivy2/local/eu.unicredit/shocon_sjs0.6_2.11/0.0.1-SNAPSHOT
$ cd ..
 
$ git clone https://github.com/unicredit/akka.js
$ cd akka.js #-> place us in merge.js branch
$ git checkout refactoring
$ git submodule init
$ git submodule update
$ sbt
> project akkaJsActorIrPatches
> compile
> publishLocal
> // publishes to $HOME/.ivy2/local/akka.js/akkajsactorirpatches_sjs0.6_2.11/0.1-SNAPSHOT
> project akkaActorJS
> publishLocal 
> //published to $HOME/.ivy2/local/akka.js/akkaactor_sjs0.6_2.11/0.2-SNAPSHOT/
```

Then download the examples and follow the README.md available [here](https://github.com/unicredit/akka.js-examples)

## Design documentation

The BSc thesis detailing most of the work and the approach taken can be found [here](pdf/thesis.pdf)

The original codebase derives from Sébastien Doeraene's `scala-js-actors`, you can find his original report [here](http://lampwww.epfl.ch/~doeraene/scalajs-actors-design.pdf).

## Akka version

As of now, code is taken from Akka 2.3.9 

## License

Akka.js is distributed under the
[Scala License](http://www.scala-lang.org/license.html).
