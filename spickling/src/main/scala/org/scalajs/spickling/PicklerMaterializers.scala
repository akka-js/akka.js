package org.scalajs.spickling

import scala.language.experimental.macros

import scala.reflect.macros.BlackboxMacro

trait PicklerMaterializersImpl extends BlackboxMacro {
  import c.universe._

  def materializePickler[T: c.WeakTypeTag]: c.Tree = {
    val tpe = weakTypeOf[T]
    val sym = tpe.typeSymbol.asClass

    if (!sym.isCaseClass) {
      c.error(c.enclosingPosition,
          "Cannot materialize pickler for non-case class")
      return q"null"
    }

    val accessors = (tpe.declarations collect {
      case acc: MethodSymbol if acc.isCaseAccessor => acc
    }).toList

    val pickleFields = for {
      accessor <- accessors
    } yield {
      val fieldName = accessor.name
      q"""
        pickle.$fieldName = registry.pickle(value.$fieldName)
      """
    }

    val pickleLogic = q"""
      val pickle = new scala.scalajs.js.Object().asInstanceOf[scala.scalajs.js.Dynamic]
      ..$pickleFields
      pickle
    """

    q"""
      implicit object GenPickler extends org.scalajs.spickling.Pickler[$tpe] {
        import org.scalajs.spickling._
        override def pickle(value: $tpe)(
            implicit registry: PicklerRegistry): scala.scalajs.js.Any = $pickleLogic
      }
      GenPickler
    """
  }
}

trait PicklerMaterializers {
  implicit def materializePickler[T]: Pickler[T] =
    macro PicklerMaterializersImpl.materializePickler[T]
}
