import scalafix.v1._
import scala.meta._

class ChangeAwaitImport extends SyntacticRule("ChangeAwaitImport") {

  def findAwaitImportee(importees: List[scala.meta.Importee]): Option[Importee] = {
    importees.find{ _ match {
      case i @ Importee.Name(Name("Await")) => true
      case _ => false
    }}
  }

  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect {
      case t @ importer"scala.concurrent.{..$importees}" =>
        findAwaitImportee(importees) match {
          case Some(i) =>
            Patch.removeImportee(i) + Patch.addRight(t, "\nimport akka.testkit.Await")
          case _ => Patch.empty
        }
    }.asPatch
  }
}
