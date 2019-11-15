package java.net

import java.io.UnsupportedEncodingException

import scala.scalajs.js

object URLEncoder {

  def encode(s: String, enc: String): String = {
    if (enc.toUpperCase != "UTF-8")
      throw new UnsupportedEncodingException(enc)
    else
      js.Dynamic.global.encodeURI(s).asInstanceOf[String]
  }

}
