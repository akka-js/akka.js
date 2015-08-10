package unicredit

import scala.tools.nsc.{ Global, Phase }
import scala.tools.nsc.plugins.{ Plugin, PluginComponent }
import scala.tools.nsc.transform.{ Transform, TypingTransformers }
import scala.tools.nsc.symtab.Flags
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.ast.TreeDSL

import java.nio.file.Files.readAllBytes
import java.nio.file.Paths.get

import scala.collection.mutable
import scala.util.{Try => STry, Success, Failure}

class MethodEraserPlugin(val global: Global) extends Plugin {
  import global._

  val name = "method-eraser-plugin"
  val description = "Want to delete method from classes by name"
  val components = List[PluginComponent](MethodEraserComponent, MethodEraserCheckComponent)

  lazy val config: mutable.Set[String] =
    (try new String(readAllBytes(get("./method_eraser.config"))).split("\n").toSeq
     catch {
       case err: Throwable =>
         println("Method eraser configuration file is missing")
         Seq()
     }).to[mutable.Set]

  private object MethodEraserCheckComponent extends PluginComponent {
    val global = MethodEraserPlugin.this.global
    import global._

    override val runsAfter = List("method-eraser")
    override val runsRightAfter = Some("method-eraser")

    val phaseName = "method-eraser-check"

    override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
      override def apply(unit: CompilationUnit) {
        config.foreach(m =>
          unit.warning(null, "METHOD ERASER ERROR: method "+m+" not found in compilation unit "+unit)
        )
      }
    }

  }

  private object MethodEraserComponent extends PluginComponent  with Transform with TreeDSL {
    val global = MethodEraserPlugin.this.global
    import global._
    import global.definitions._

    override val runsAfter = List("namer")
    // override val runsRightAfter = Some("namer") // impossible due to `packageobjects` phase

    val phaseName = "method-eraser"

    def newTransformer(unit: CompilationUnit) =
      new AggregateEraserTransformer(unit)

    class AggregateEraserTransformer(unit: CompilationUnit) extends Transformer {

      val erasers = config.flatMap { (m: String) =>
        val method = STry { // create method symbol from `m`
          val i = m.lastIndexOf('.')
          val className = m.substring(0, i)
          val methodName = m.substring(i + 1)
          //println(s"className: $className")
          //println(s"methodName: $methodName")
          // TODO: we might select a method of an object
          val cl = rootMirror.getClassByName((className: TypeName))
          getMemberMethod(cl, (methodName: TermName))
        }
        method match {
          case Success(methodSym) =>
            Seq(new EraserTransformer(unit, m, methodSym))
          case Failure(e) =>
            println(s"method '$m' does not exist")
            Seq()
        }
      }

      override def transform(tree: Tree): Tree = {
        val iter = erasers.iterator
        var count = 0
        while(iter.hasNext && !iter.next.check(tree)) {
          count += 1
        }
        if (count == erasers.size)
          super.transform(tree)
        else
          Literal(Constant(())) setType UnitTpe
      }
    }

    class EraserTransformer(unit: CompilationUnit, initMethodName: String, methodSym: TermSymbol) {

      def check(tree: Tree): Boolean = {
        tree match {
          case dd @ DefDef(Modifiers(flags, privateWithin, annotations), name, tparams, vparamss, tpt, rhs)
            if (methodSym == dd.symbol) =>
            //unit.warning(tree.pos, "METHOD ERASED")
            config -= initMethodName
            true
          case any =>
            false
        }
      }

    }
  }
}
