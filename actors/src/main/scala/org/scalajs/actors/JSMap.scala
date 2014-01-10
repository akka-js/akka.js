package org.scalajs.actors

import scala.collection.mutable.{ Map, MapLike }

import scala.scalajs.js
import js.annotation.JSBracketAccess

final class JSMap[A] private () extends Map[String, A]
                                   with MapLike[String, A, JSMap[A]] {
  private[this] val dict: js.Dictionary = js.Dictionary.empty

  override def empty: JSMap[A] = new JSMap[A]

  override def get(key: String): Option[A] = {
    val value = dict(key)
    if (value.isInstanceOf[js.Undefined]) None
    else Some(value.asInstanceOf[A])
  }

  override def +=(kv: (String, A)): this.type = {
    assert(!kv._2.isInstanceOf[js.Undefined], "Cannot put undefined in JSMap")
    dict(kv._1) = kv._2.asInstanceOf[js.Any]
    this
  }

  override def -=(key: String): this.type = {
    // TODO Actually use the 'delete' instruction of JavaScript
    dict(key) = ((): js.Undefined)
    this
  }

  override def iterator: Iterator[(String, A)] = {
    for {
      key <- js.Object.keys(dict.asInstanceOf[js.Object]).iterator
      value = dict(key)
      if !value.isInstanceOf[js.Undefined]
    } yield {
      (key, value.asInstanceOf[A])
    }
  }
}

object JSMap {
  def empty[A]: JSMap[A] = new JSMap[A]
}
