package akka

package object util {

  implicit class toLowerWithLocale(s: java.lang.String) {
    def toLowerCase(l: java.util.Locale) = s.toLowerCase()
  }

}
