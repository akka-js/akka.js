![Akka.Js](https://raw.githubusercontent.com/unicredit/akka.js/merge-js/logo/akkajs.png)

[![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.8.svg)](https://www.scala-js.org)

[![Join the chat at https://gitter.im/unicredit/akka.js](https://badges.gitter.im/unicredit/akka.js.svg)](https://gitter.im/unicredit/akka.js?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This repository is an ongoing effort to port Akka to the JavaScript runtime, thanks to [Scala.js](http://scala-js.org)

[LIVE DEMO](https://unicredit.github.io/akka.js/)

## Use it

To have a blazing fast kick off you can check out our [examples](https://github.com/unicredit/akka.js-examples).

Otherwise, if you want to start from scratch:

First of all you need to setup a new [Scala.js project](https://www.scala-js.org/doc/project/).
Then add to your JS project configuration:
```scala
resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies += "eu.unicredit" %%% "akkajsactor" % "0.2.0"
```

If you want to use Akka Stream:
```scala
libraryDependencies += "eu.unicredit" %%% "akkajsactorstream" % "0.2.0"
```

Please note that Akka.js 0.2.0 is shipped from Akka 2.4.9-RC2.
At this point you can use most of the Akka core Api as described in the official [docs](http://doc.akka.io/docs/akka/2.4.9-RC2/scala.html).

Check out the @andreaTP session at Scala Days 2016:
[slides](https://github.com/andreaTP/sd2016.git)
[video](https://youtu.be/OCbuOc1GRP8)

## Design documentation

The BSc thesis detailing most of the work and the approach taken can be found [here](../../blob/merge-js/pdf/thesis.pdf)

The original codebase derives from Sébastien Doeraene's `scala-js-actors`, you can find his original report [here](http://lampwww.epfl.ch/~doeraene/scalajs-actors-design.pdf).

## Build it

To work with the very last version you can compile and publish local:
```
git clone https://github.com/unicredit/akka.js
cd akka.js
sbt akkaJsActorJS/publishLocal
```
To have also the bleeding edge integration of akka-stream:
```
sbt akkaJsActorStreamJS/publishLocal
```

## Akka version

Akka.Js can now compile against different versions of Akka, we check the codebase against MASTER,
but for specific needs you can try to compile against a different Akka version by changing the akkaVersion while building.

## License

Akka.js is distributed under the
[Scala License](http://www.scala-lang.org/license.html).
