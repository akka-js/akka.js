package akka.annotation

import scala.annotation.StaticAnnotation

class ApiMayChange extends StaticAnnotation

class DoNotInherit extends StaticAnnotation

class InternalApi extends StaticAnnotation

class InternalStableApi extends StaticAnnotation
