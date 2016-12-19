![Akka.Js](https://raw.githubusercontent.com/unicredit/akka.js/merge-js/logo/akkajs.png)

[![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.8.svg)](https://www.scala-js.org)

[![Join the chat at https://gitter.im/unicredit/akka.js](https://badges.gitter.im/unicredit/akka.js.svg)](https://gitter.im/unicredit/akka.js?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This repository is the port of sources Akka to the JavaScript runtime, thanks to [Scala.js](http://scala-js.org)

[LIVE DEMO](http://akka-js.org)

## Use it

To have a blazing fast kick off you can check out our [examples](https://github.com/unicredit/akka.js-examples).

Otherwise, if you want to start from scratch:

First of all you need to setup a new [Scala.js project](https://www.scala-js.org/doc/project/).
Then add to your JS project configuration:
```scala
libraryDependencies += "org.akka-js" %%% "akkajsactor" % "0.2.4.16"
```

If you want to use Akka Stream:
```scala
libraryDependencies += "org.akka-js" %%% "akkajsactorstream" % "0.2.4.16"
```

To test your code you can use:
```scala
libraryDependencies += "org.akka-js" %%% "akkajstestkit" % "0.2.4.16"
libraryDependencies += "org.akka-js" %%% "akkajsstreamtestkit" % "0.2.4.16"
```


Please note that Akka.js 0.2.4.16 is shipped from the stable Akka 2.4.16.
At this point you can use most of the Akka core Api as described in the official [docs](http://doc.akka.io/docs/akka/2.4.16/scala.html).

Check out the @andreaTP session at Scala Days 2016:
[slides](https://github.com/andreaTP/sd2016.git)
[video](https://youtu.be/OCbuOc1GRP8)

Or @andreaTP session at BeeScala 2016:
[slides](https://github.com/andreaTP/beescala.git)
[video](https://youtu.be/pO1rY5780Mg)

## Caveats

There are small caveats to keep in mind to ensure that your code will run smoothly on both Jvm and Js.

***Startup Time***

On Js VM we cannot block, so to ensure your code will run AFTER the ```ActorSystem``` scheduler is started you need to run your code within a block like this:
```scala
import system.dispatcher
import scala.concurrent.duration._
system.scheduler.scheduleOnce(0 millis){
  ... your code here ...
}
```

***Reflective Actor Instatiation***

On JVM you are used instatiating your actors like:
```scala
system.actorOf(Props(classOf[MyActor]))
```
Unfortunately this wont work out of the box on Scala.Js.
The easiest way to fix it is to use the ```non-reflective``` constructor:
```scala
system.actorOf(Props(new MyActor()))
```
If you really want to use the reflective one you need two steps.

  - Verify that your class is exported to JavaScript properly marking it with ```JSExport``` and ensuring there are no compile-time errors (you can remove the annotation after the check)
  - Add it to the list of dynamically loadable actors before starting the ActorSystem:
  ```scala
  akka.actor.JSDynamicAccess.injectClass("StringClassNameOfT" -> classOf[T])
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
