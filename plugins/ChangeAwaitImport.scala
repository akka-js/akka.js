import scalafix.v1._
import scala.meta._

class ChangeAwaitImport extends SyntacticRule("ChangeAwaitImport") {


  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree
      .collect {
        case t @ q"import scala.concurrent.Await" =>
          Patch.replaceTree(t, "akka.testkit.Await")
      }
      .asPatch
  }
}

