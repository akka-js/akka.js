package java.text

import java.util.Date

class SimpleDateFormat(fmt: String) {

  def format(date: Date): String = {
    val day = trailingZeros(2, date.getDay)
    val month = trailingZeros(2, date.getMonth)
    val year = trailingZeros(4, date.getYear)

    val hours = trailingZeros(2, date.getHours)
    val minutes = trailingZeros(2, date.getMinutes)
    val seconds = trailingZeros(2, date.getSeconds)

    val millis = trailingZeros(3,
      date.getTime -
        Date.UTC(date.getYear,
                 date.getMonth,
                 date.getDay,
                 date.getHours,
                 date.getMinutes,
                 date.getSeconds)
      )

    s"$day/$month/$year $hours:$minutes:$seconds.$millis"
  }

  private def trailingZeros(digits: Int, value: Long) = {

    def compose(digit: Int, str: Array[Char]): Array[Char] =
      if (digit > 0) {
        if (str.isEmpty) Array('0')
        else compose(digit - 1, str.tail) ++ Array(str.head)
      } else Array(0.toChar)

    new String(compose(digits, value.toString.toCharArray.reverse))
  }
}
