package akka.pattern;

import akka.util.Unsafe

class AbstractCircuitBreaker {}

object AbstractCircuitBreaker {
    final val stateOffset = 6
    final val resetTimeoutOffset = 7
}
