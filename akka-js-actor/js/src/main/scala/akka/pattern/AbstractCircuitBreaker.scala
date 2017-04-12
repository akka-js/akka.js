package akka.pattern;

import akka.util.Unsafe

class AbstractCircuitBreaker {}

object AbstractCircuitBreaker {
    final val stateOffset = 40L
    final val resetTimeoutOffset = 41L
}
