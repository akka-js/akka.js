package org.scalajs.spickling

import scala.language.experimental.macros

import scala.reflect.macros.BlackboxContext

object PicklerMaterializersImpl {
  def materializePickler[T: c.WeakTypeTag](c: BlackboxContext): c.Tree = {
    import c.universe._

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

  def materializeUnpickler[T: c.WeakTypeTag](c: BlackboxContext): c.Tree = {
    import c.universe._

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

    val unpickledFields = for {
      accessor <- accessors
    } yield {
      val fieldName = accessor.name
      val fieldTpe = accessor.returnType
      q"""
        registry.unpickle(pickle.$fieldName).asInstanceOf[$fieldTpe]
      """
    }

    val unpickleLogic = q"""
      val pickle = json.asInstanceOf[scala.scalajs.js.Dynamic]
      new $tpe(..$unpickledFields)
    """

    q"""
      implicit object GenUnpickler extends org.scalajs.spickling.Unpickler[$tpe] {
        import org.scalajs.spickling._
        override def unpickle(json: scala.scalajs.js.Any)(
            implicit registry: PicklerRegistry): $tpe = $unpickleLogic
      }
      GenUnpickler
    """
  }

  def materializeCaseObjectName[T: c.WeakTypeTag](c: BlackboxContext): c.Tree = {
    import c.universe._

    val tpe = weakTypeOf[T]
    val sym = tpe.typeSymbol.asClass

    if (!sym.isModuleClass || !sym.isCaseClass)
      c.abort(c.enclosingPosition,
          s"Cannot generate a case object name for non-case object $sym")

    val name = sym.fullName+"$"
    q"""
      new org.scalajs.spickling.PicklerRegistry.SingletonFullName($name)
    """
  }
}

trait PicklerMaterializers {
  implicit def materializePickler[T]: Pickler[T] =
    macro PicklerMaterializersImpl.materializePickler[T]

  implicit def materializeUnpickler[T]: Unpickler[T] =
    macro PicklerMaterializersImpl.materializeUnpickler[T]

  implicit def materializeCaseObjectName[T]: PicklerRegistry.SingletonFullName[T] =
    macro PicklerMaterializersImpl.materializeCaseObjectName[T]
}
