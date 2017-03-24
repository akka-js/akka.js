/**
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package akka.routing

import scala.scalajs.reflect.annotation.EnableReflectiveInstantiation

//this is needed just to have a class instance public and exportable
@EnableReflectiveInstantiation
class JSRouterActorCreator(routerConfig: RouterConfig) extends RoutedActorCell.RouterActorCreator(routerConfig)
