package org.scalajs.actors.util

import scala.annotation.tailrec

object Helpers {

  final val base64chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+~"

  @tailrec
  def base64(l: Long, prefix: String = "$"): String = {
    val newPrefix = prefix + base64chars.charAt(l.toInt & 63)
    val next = l >>> 6
    if (next == 0) newPrefix
    else base64(next, newPrefix)
  }

}
