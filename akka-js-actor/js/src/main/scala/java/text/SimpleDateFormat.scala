package java.text

import java.util.Date

class SimpleDateFormat(fmt: String) {

  def format(date: Date): String = {
    //can be done better...
    date.toString
  }
}