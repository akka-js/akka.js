package akka.annotation

import scala.annotation.StaticAnnotation

class ApiMayChange extends StaticAnnotation {
  def this(issue: String) = this()
}

class DoNotInherit extends StaticAnnotation

class InternalApi extends StaticAnnotation

class InternalStableApi extends StaticAnnotation
