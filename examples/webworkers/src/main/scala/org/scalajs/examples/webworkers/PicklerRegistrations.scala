package org.scalajs.examples.webworkers

import org.scalajs.spickling._

object PicklerRegistrations {
  def registerTo(registry: BasePicklerRegistry): Unit = {
    import registry.register

    register[Greeting]
  }
}
