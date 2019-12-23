![Akka.Js](https://raw.githubusercontent.com/unicredit/akka.js/merge-js/logo/akkajs.png)

[![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.8.svg)](https://www.scala-js.org)
[![Join the chat at https://gitter.im/akka-js/akka.js](https://badges.gitter.im/akkajs/Lobby.svg)](https://gitter.im/akkajs/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=body_badge)
[![Build Status](https://travis-ci.org/akka-js/akka.js.svg?branch=master)](https://travis-ci.org/akka-js/akka.js)

This repository is the port of sources Akka to the JavaScript runtime, thanks to [Scala.js](http://scala-js.org)

[LIVE DEMO](http://akka-js.org)

## Use it

To have a blazing fast kick off you can check out our [examples](https://github.com/unicredit/akka.js-examples).

Otherwise, if you want to start from scratch:

First of all you need to setup a new [Scala.js project](https://www.scala-js.org/doc/project/).
Then add to your JS project configuration:
```scala
libraryDependencies += "org.akka-js" %%% "akkajsactor" % "1.2.6.1"
```

If you want to use Akka Stream:
```scala
libraryDependencies += "org.akka-js" %%% "akkajsactorstream" % "1.2.6.1"
```

To test your code you can use:
```scala
libraryDependencies += "org.akka-js" %%% "akkajstestkit" % "1.2.6.1" % "test"
libraryDependencies += "org.akka-js" %%% "akkajsstreamtestkit" % "1.2.6.1" % "test"
```

You can also use Akka Typed:
```scala
libraryDependencies += "org.akka-js" %%% "akkajsactortyped" % "1.2.6.1"
libraryDependencies += "org.akka-js" %%% "akkajstypedtestkit" % "1.2.6.1" % "test"
```

And Akka Stream Typed interface:
```scala
libraryDependencies += "org.akka-js" %%% "akkajsactorstreamtyped" % "1.2.6.1"
```


Please note that Akka.js 1.2.6.1 is shipped from the stable Akka 2.6.1.
At this point you can use most of the Akka core Api as described in the official [docs](http://doc.akka.io/docs/akka/2.6.1/scala.html).

Check out the @andreaTP session at Scala Days 2016:
[slides](https://github.com/andreaTP/sd2016.git)
[video](https://youtu.be/OCbuOc1GRP8)

Or @andreaTP session at BeeScala 2016:
[slides](https://github.com/andreaTP/beescala.git)
[video](https://youtu.be/pO1rY5780Mg)

@andreaTP at ScalaUA 2017:
[slides](https://github.com/andreaTP/scalaua.git)
[video](https://youtu.be/4nsVfi6e0uM)

@andreaTP at ScalaSwarm 2017:
[slides](https://github.com/andreaTP/scalaswarm.git)
[video](https://youtu.be/cMcOf6f2EI0)


## Caveats

There are small caveats to keep in mind to ensure that your code will run smoothly on both Jvm and Js.

***Startup Time***

On Js VM the control flow will execute first all operations in line, so to ensure your code will run AFTER the ```ActorSystem``` is started you need to run your code within a block like this:

```scala
import system.dispatcher
import scala.concurrent.duration._
system.scheduler.scheduleOnce(0 millis){
  ... your code here ...
}
```

***Reflective Actor Instatiation***

Since Scala.Js 0.6.15 reflective class instatiation is perfectly supported.

***Testing***

To handle *blocking* testing in test suites you cannot use modules that interact with external world.
Is therefore strictly prohibited to use any different `ExecutionContext` (i.e. you cannot `import scala.concurrent.ExecutionContext.Implicits.global`).

***Mailbox configuration***

Due to the lack of runtime reflection in Scala.Js is not possible to configure mailbox of actors using __requirements__ but is it possible to do so with direct configuration.
i.e.

```scala
lazy val config: Config =
  ConfigFactory
    .parseString("""
      stash-custom-mailbox {
        mailbox-type = "akka.dispatch.UnboundedDequeBasedMailbox"
      }
      """
    ).withFallback(akkajs.Config.default)

val system: ActorSystem = ActorSystem("wsSystem", config)

val actorWithStash =
  system.actorOf(Props(new ActorWithStash()).withMailbox("stash-custom-mailbox"), "ActorWithStash")
```

## Add-ons

Since semantics difference to Akka on JVM we include a bunch of helpers to make life easier:

```scala
akkajs.Config.default // default configuration shipped with Akka.JS

akka.testkit.TestKitBase {
  await() // to help on waiting during tests
  await(duration: Long) // with configurable duration
}
```

To change default configuration logging level you can simply:

```scala
lazy val conf =
  ConfigFactory
    .parseString("""
    akka {
      loglevel = "DEBUG"
      stdout-loglevel = "DEBUG"
    }""")
    .withFallback(akkajs.Config.default)

lazy val system = ActorSystem("yourname", conf)
```

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
To have also akka-stream:
```
sbt akkaJsActorStreamJS/publishLocal
```
For the bleeding edge akka-typed
```
sbt akkaJsActorTypedJS/publishLocal
```
## Built with Akka.js

  - [akka-ui](https://github.com/pishen/akka-ui) FRP with Akka.js

## Akka version

Akka.Js can now compile against different versions of Akka, we check the codebase against MASTER,
but for specific needs you can try to compile against a different Akka version by changing the akkaVersion while building.

## Credits

Akka.js wouldn't have been possible without the enormous support of the R&D department of UniCredit lead by Riccardo Prodam. What started as a side-project for a master thesis grew into an important open source milestone.
Check out other projects from the unicredit team [here](https://github.com/unicredit)

## License

Akka.js is distributed under the
[Scala License](http://www.scala-lang.org/license.html).
