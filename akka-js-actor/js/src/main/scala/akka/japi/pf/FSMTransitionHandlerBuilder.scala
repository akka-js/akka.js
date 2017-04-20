package akka.japi.pf

class FSMTransitionHandlerBuilder[S] {

  def build() =
    new PartialFunction[(S, S), Unit]{
      def apply(v1: (S, S)): Unit = ()
      def isDefinedAt(x: (S, S)): Boolean = false
    }
}
