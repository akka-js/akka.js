package akka.util

import scala.collection.mutable.{ Map, MapLike }

import scala.scalajs.js
import js.annotation.JSBracketAccess

final class JSMap[A] private () extends Map[String, A]
                                   with MapLike[String, A, JSMap[A]] {

  private[this] val dict: js.Dictionary[A] = js.Dictionary.empty[A]

  override def empty: JSMap[A] = new JSMap[A]

  override def get(key: String): Option[A] = {
    if (dict.isDefinedAt(key)) Some(dict(key))
    else None
  }

  override def +=(kv: (String, A)): this.type = {
    dict(kv._1) = kv._2
    this
  }

  override def -=(key: String): this.type = {
    dict.delete(key)
    this
  }

  override def iterator: Iterator[(String, A)] = {
    for {
      key <- dict.keys.iterator
    } yield {
      (key, dict(key))
    }
  }
}

object JSMap {
  def empty[A]: JSMap[A] = new JSMap[A]
}
