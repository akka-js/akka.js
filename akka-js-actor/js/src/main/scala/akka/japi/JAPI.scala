package akka.japi

import scala.collection.Seq
import scala.collection.JavaConversions._

object JAPI {

  def seq[T](ts: T*): Seq[T] =
    Seq(ts: _*)
    //Util.immutableSeq(ts.toSeq.iterator)

}
