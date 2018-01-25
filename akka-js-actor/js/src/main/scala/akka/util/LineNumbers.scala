package akka.util

object LineNumbers {

  def apply(mock: Any) = new LineNumbers(mock)

}

class LineNumbers(mock: Any) {
  override def toString() = "LN"
}
