package akka.util

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object Unsafe {

  def getObjectVolatileImpl(c: Context)(o: c.Expr[Any], offset: c.Expr[Int]): c.Expr[AnyRef] = {
    import c.universe._

    c.info(c.enclosingPosition, s"let see ${offset.tree}", true)

    offset.tree match {
      case q"42" =>
        c.Expr[AnyRef](q"""{
          type WithFieldXYZ = {
            var XYZ: Int
          }

          try {
            $o.asInstanceOf[WithFieldXYZ].XYZ += 1
          } catch {
            case _: Throwable => println("?@?@")
          }

          println("yes sir")

          $o.asInstanceOf[WithFieldXYZ].XYZ.asInstanceOf[AnyRef]
        }""")
      case x =>
        c.Expr[AnyRef](q"""{
          type VarPollution = {
            var _cellDoNotCallMeDirectly: Int
          }

          $o.asInstanceOf[VarPollution]._cellDoNotCallMeDirectly = 42

          println("doh")
          "???"
        }""")
    }
  }

  object Instance {

    def getObjectVolatile(o: Any, offset: Int): AnyRef =
      macro getObjectVolatileImpl

  }

  final val instance = Instance

}
