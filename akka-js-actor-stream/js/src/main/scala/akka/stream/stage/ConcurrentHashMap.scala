package akka.stream.stage

object ConcurrentHashMap {

  def newKeySet[K]() =
    (new java.util.concurrent.ConcurrentHashMap[K, Boolean]).keySet()

}
