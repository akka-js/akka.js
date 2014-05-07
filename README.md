# Actor system for Scala.js

This project aims at providing an actor system for Scala.js, featuring:

*   Supervision
*   (Almost) transparent collaboration with actors in an Akka-based backend
*   Transparent collaboration with actors in several Web Workers

It is currently a prototype, working well but still in a rough shape. Hence,
it is not published anywhere yet.

## Build it and try the examples

To build the libraries, use

    > package

in an sbt console.

### Fault-tolerance example

The fault-tolerance example is taken from Akka's documentation and showcases
supervision, death watch notifications and the like.

Build it with:

    > faultToleranceExample/fastOptJS

then open `examples/faulttolerance/index-fastopt.html` in your browser and look
at the Web console for the output.

### Web Workers example

The Web Workers example demonstrates the communication between Web Workers.

Build it with:

    > webworkersExample/fastOptJS

then open `examples/webworkers/index-fastopt.html` in your browser and look at
the Webconsole for the output.

### Chat example (with client-server communication)

The Chat example is a full-stack Play/Akka/Scala.js application where client
and server communicate transparently between Akka/JVM and "Akka/JS".

Build the client then run the server with:

    > chatExampleScalaJS/fullOptJS
    > project chatExample
    [scalajs-actors-examples-chat] $ run

then navigate to [http://localhost:9000/opt](http://localhost:9000/opt) with
your browser. To have some fun, open multiple tabs (or multiple browsers) and
start playing with the chat.

## Design documentation

The best source of documentation for the design at large is
[this report](http://lampwww.epfl.ch/~doeraene/scalajs-actors-design.pdf).

## License

Scala.js actors is distributed under the
[Scala License](http://www.scala-lang.org/license.html).
