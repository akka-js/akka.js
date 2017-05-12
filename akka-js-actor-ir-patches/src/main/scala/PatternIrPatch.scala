package akka.pattern

trait State {}

private[akka] final class PromiseActorRef {

  var stateCallMeDirectly: AnyRef = _
  var watchedByCallMeDirectly: Set[ActorRef] = _
}

class CircuitBreaker {

  var currentStateCallMeDirectly: State = _
  var currentResetTimeoutCallMeDirectly: FiniteDuration = _
}
