package akka

import com.typesafe.config.Config

package object persistence {

  //to be implemented in shocon
  implicit class ConfigLongMock(c: Config) {
    def getLong(key: String): Long =
      c.getString(key).toLong
  }
}
