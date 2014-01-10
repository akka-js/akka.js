package org.scalajs.examples.webworkers

import org.scalajs.spickling._

object PicklerRegistrations {
  def registerTo(registry: PicklerRegistry): Unit = {
    import registry.register

    register[Greeting]
  }
}
