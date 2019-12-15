package akka

import com.typesafe.config.{ConfigFactory, Config}

package object util {

  implicit class toLowerWithLocale(s: java.lang.String) {
    def toLowerCase(l: java.util.Locale) = s.toLowerCase()
  }

  implicit class ConfigExtension(c: Config) {
    def withoutPath(str: String): Config = {
      ConfigFactory.parseString(s"""{ $str = null }""").withFallback(c)
    }
  }

}
