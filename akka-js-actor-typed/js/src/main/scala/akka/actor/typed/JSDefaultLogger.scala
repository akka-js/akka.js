package akka.actor.typed

import akka.{ event ⇒ e }
import akka.event.Logging.{ LogEvent, LogLevel, StdOutLogger }
import akka.event.typed.Logger

object JSDefaultLogger { // extends Logger with StdOutLogger {
  import Logger._

  val initialBehavior = {
    // TODO avoid depending on dsl here?
    import scaladsl.Actor._
    deferred[Command] { _ ⇒
      immutable[Command] {
        case (ctx, Initialize(eventStream, replyTo)) ⇒
          val log = ctx.spawn(deferred[AnyRef] { childCtx ⇒

            immutable[AnyRef] {
              case (_, event: LogEvent) ⇒
                print(event)
                same
              case _ ⇒ unhandled
            }
          }, "logger")

          ctx.watch(log) // sign death pact
          replyTo ! log

          empty
      }
    }
  }
}
