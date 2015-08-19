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
import scala.util.{ Try => STry, Success, Failure }
import scala.reflect.internal.MissingRequirementError

class AnnotationAdderPlugin(val global: Global) extends Plugin {
  import global._

  val name = "annotation-adder-plugin"
  val description = "Want to add annotation to classes, fields, and methods"
  val components = List[PluginComponent](AnnotationAdderComponent)

  lazy val config: mutable.Set[(String, String, List[String])] =
    (try new String(readAllBytes(get("./annotation_adder.config"))).split("\n").toSeq.map(e => {
      val splitted = e.split(" ")
      (splitted(0), splitted(1), splitted.drop(2).toList)
    })
    catch {
      case err: Throwable =>
        println("Annotation adder configuration file is missing")
        Seq()
    }).to[mutable.Set]

  private object AnnotationAdderComponent extends PluginComponent with Transform {
    val global = AnnotationAdderPlugin.this.global
    import global._
    import global.definitions._

    override val runsAfter = List("typer")

    val phaseName = "annotation-adder"

    def newTransformer(unit: CompilationUnit) =
      new Transformer {

        val annotators =
          config.flatMap { c =>
            val originalSym = STry {
              try
                rootMirror.getClassByName((c._1: TypeName))
              catch {
                case _: MissingRequirementError => //is not a class get member
                  val i = c._1.lastIndexOf('.')
                  val className = c._1.substring(0, i)
                  val memberName = c._1.substring(i + 1)

                  val cl = rootMirror.getClassByName((className: TypeName))
                  try {
                     getMember(cl, (memberName: TermName))  
                  } catch {
                    case err: Throwable =>
                      err.printStackTrace
                      throw err
                  }
                case err: Throwable =>
                  err.printStackTrace
                  throw err
              }
            }
            val annotation = STry {

                rootMirror.getClassByName((c._2: TypeName))
            }
            
            val params = 
                c._3.map(x => { //maybe a little better could be done
                  x match {
                    case "true" => reify(true).tree
                    case "false" => reify(false).tree
                    case str => reify(str.toString()).tree
                  }
                })

            (originalSym, annotation) match {
              case (Success(orSym), Success(annotationSym)) =>
                //orSym.addAnnotation(annotationSym)
                Some(orSym, annotationSym, params)
              //unit.warning(null, s"adding annotation ${c._2} to ${c._1}")
              case _ =>
                None
              //unit.warning(null, s"ANNOTATION ADDER ERROR: ${c._1} or ${c._2} not found")
            }
          }

        //probably we could avoid to use a transformer and use a traver only
        override def transform(tree: Tree): Tree = {

  /*       // try {
           val toAnnotate = annotators.find { case (symb,_,_) => symb == tree.symbol && symb.owner == tree.symbol.owner}

            val toValAnnotate = 
              if (toAnnotate.isDefined) None
              else annotators.find { case (symb,_,_) => 
                symb.owner == tree.symbol.owner && (symb.nameString+" " : TermName) == tree.symbol.name}
*/
            tree match {
              case cd : ClassDef =>
                val toAnnotate = annotators.find { case (symb,_,_) => 
                  symb == cd.symbol && symb.owner == cd.symbol.owner}
                if (toAnnotate.isDefined) {
                  cd.symbol.addAnnotation(toAnnotate.get._2, toAnnotate.get._3: _*)
                  unit.warning(tree.pos, s"Class Annotation ${toAnnotate.get._2.nameString} added.")
                }
              case dd: DefDef =>
                val toAnnotate = annotators.find { case (symb,_,_) => 
                  symb == dd.symbol && symb.owner == dd.symbol.owner}
                if (toAnnotate.isDefined) {
                  dd.symbol.addAnnotation(toAnnotate.get._2, toAnnotate.get._3: _*)
                  unit.warning(tree.pos, s"Def Annotation ${toAnnotate.get._2.nameString} added.")
                }
              case vd: ValDef =>
                val toAnnotate = annotators.find { case (symb,_,_) => 
                  symb.owner == vd.symbol.owner && (symb.nameString+" " : TermName) == vd.symbol.name}
                if (toAnnotate.isDefined) {
                  vd.symbol.addAnnotation(toAnnotate.get._2, toAnnotate.get._3: _*)
                  unit.warning(tree.pos, s"Val Annotation ${toAnnotate.get._2.nameString} added.")
                }
              case _ =>
            }
/*
            if (toAnnotate.isDefined) {
              tree.symbol.addAnnotation(toAnnotate.get._2, toAnnotate.get._3: _*)
              unit.warning(tree.pos, s"Annotation ${toAnnotate.get._2.nameString} added.")
            } else if (toValAnnotate.isDefined) {
              tree.symbol.addAnnotation(toValAnnotate.get._2, toAnnotate.get._3: _*)
              unit.warning(tree.pos, s"Annotation ${toValAnnotate.get._2.nameString} added")
            }

          } catch {
            case _ : Throwable =>
          }*/
          
          super.transform(tree)
        }
      }
  }
}
