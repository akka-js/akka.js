package akka.util

import scala.scalajs.js

@js.native
@js.annotation.JSName("WeakMap")
class WeakMap[K <: AnyRef, V] extends js.Object {

  def delete(key: K): Unit = js.native

  def has(key: K): Boolean = js.native

  def get(key: K): V = js.native

  def set(key: K, value: V): Unit = js.native
}
