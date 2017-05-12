package akka.pattern

private[akka] final class PromiseActorRef {

  var stateCallMeDirectly: AnyRef = _
  var watchedByCallMeDirectly: Set[akka.actor.ActorRef] = _
}

class CircuitBreaker {

  trait State {}

  var currentStateCallMeDirectly: State = _
  var currentResetTimeoutCallMeDirectly: scala.concurrent.duration.FiniteDuration = _
}
