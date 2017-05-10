package something

import akka.util.Unsafe

import scala.scalajs.js

object Main extends js.JSApp {

  def main()= {
    class Something {

      /*@volatile private*/ var _cellDoNotCallMeDirectly: Int = 5
      var XYZ: Int = 1

      def show() = println(_cellDoNotCallMeDirectly)
    }

    val s = new Something()

    val res = {
      Unsafe.instance.getObjectVolatile(s, AbstractSomething.ft)
      Unsafe.instance.getObjectVolatile(s, AbstractSomething.ft)
    }

    s.show()
    Unsafe.instance.getObjectVolatile(s, AbstractSomething.again)
    s.show()

    println(res)
  }
}


object AbstractSomething {

  final val ft = 42

  final val again = 1

}
