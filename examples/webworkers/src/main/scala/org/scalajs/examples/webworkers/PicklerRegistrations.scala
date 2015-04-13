package org.scalajs.examples.webworkers

import be.doeraene.spickling._

object PicklerRegistrations {
  def registerTo(registry: BasePicklerRegistry): Unit = {
    import registry.register

    register[Greeting]
  }
}
