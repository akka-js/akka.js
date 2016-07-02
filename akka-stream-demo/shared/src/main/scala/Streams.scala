package eu.unicredit


import akka.actor._
import akka.stream.actor.{ActorPublisherMessage, ActorPublisher}
import akka.stream.scaladsl._
import akka.stream._
import akka.util.Timeout
import scala.annotation.tailrec
import akka.pattern.ask
import scala.concurrent.Future
import scala.concurrent.duration._

object Streams {


  class ToStringActor extends Actor {
    def receive = {
      case value => sender () ! s"[${value.toString}]"
    }
  }

  class ToStringActorPublisher extends ActorPublisher[String] {

    val MaxBufferSize = 100
    var buf = Vector.empty[String]

    def receive = {
      case value: Int  =>
        if (buf.isEmpty && totalDemand > 0)
          onNext(value.toString())
        else {
          buf :+= value.toString
          deliverBuf()
        }
      case ActorPublisherMessage.Request(_) =>
        deliverBuf()
      case ActorPublisherMessage.Cancel =>
        context.stop(self)
    }

    @tailrec final def deliverBuf(): Unit =
      if (totalDemand > 0) {
        /*
         * totalDemand is a Long and could be larger than
         * what buf.splitAt can accept
         */
        if (totalDemand <= Int.MaxValue) {
          val (use, keep) = buf.splitAt(totalDemand.toInt)
          buf = keep
          use foreach onNext
        } else {
          val (use, keep) = buf.splitAt(Int.MaxValue)
          buf = keep
          use foreach onNext
          deliverBuf()
        }
      }
  }
  def complexFlow(implicit system: ActorSystem) = {

    val log = system.log

    implicit val materializer = ActorMaterializer()

    val toStringActor = system.actorOf(Props(new ToStringActor()), "toStringActor")

    val factorial = Source(1 to 10).scan(1)(_ * _)

    val strings = Source(1 to 10).map( _.toString)

    implicit val dispatcher = system.dispatcher
    implicit val timeout = Timeout(5 seconds)

    val strings2 =  Source(1 to 10).mapAsync(4)(value => (toStringActor ? value).mapTo[String])

    val throttledAndZipped = Flow[String]
      .zip(factorial)
      .throttle(1, 1.second, 1, ThrottleMode.shaping)
      .mapAsync(10)(a=>Future(s"${new java.util.Date()} - $a"))
      .to(Sink.foreach(println))

    //val toStringActorRef = throttledAndZipped.runWith(toStringActorPublisherSource)


    throttledAndZipped.runWith(strings)
    log.debug("runwith string")


    system.scheduler.scheduleOnce(12 second)(
      system.terminate()
    )


    //(1 to 10).map ( toStringActorRef ! _)

  }


  def simpleFlow(implicit system: ActorSystem) = {
    val log = system.log

    import akka.stream.ActorMaterializer
    import akka.stream.scaladsl._


    implicit val dispatcher = system.dispatcher
    implicit val materializer = ActorMaterializer()

    Source(List("Hello World", "Wo am I"))
      .map((s: String) => s + "!")
      .runWith(Sink.foreach(println))
      .onComplete {
        case _ => log.info("Complete")
      }
  }


}
