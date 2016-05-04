package java.util

class Locale(language: String, country: String, variant: String) {

  def this(language: String) = this(language, null, null)

  def this(language: String, country: String) = this(language, country, null)
}

object Locale {

  final val ROOT: Locale = new Locale("ROOT")

}
