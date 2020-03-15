import scalafix.v1._
import scala.meta._

class ChangeAwaitImport extends SyntacticRule("ChangeAwaitImport") {


  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree
      .collect {
        case t @ q"import scala.concurrent.Await" =>
          Patch.replaceTree(t, "import akka.testkit.Await")
        case t @ q"import scala.concurrent.{ Promise, Await }" =>
          Patch.replaceTree(t, "import scala.concurrent.Promise\nimport akka.testkit.Await")
        case t @ q"import scala.concurrent.{ Await, Promise }" =>
          Patch.replaceTree(t, "import scala.concurrent.Promise\nimport akka.testkit.Await")
      }
      .asPatch
  }
}

