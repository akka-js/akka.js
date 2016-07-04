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
      case value:String  =>
        if (buf.isEmpty && totalDemand > 0)
          onNext(value)
        else {
          buf :+= value
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
  def complexFlow(runActorPublisher: Boolean) (implicit system: ActorSystem) = {

    val log = system.log

    implicit val materializer = ActorMaterializer()

    val toStringActor = system.actorOf(Props(new ToStringActor()), "toStringActor")

    val toStringActorPublisherSource =
      if (runActorPublisher)
        Source.actorPublisher[String](Props[ToStringActorPublisher])
      else
        Source.actorRef[String](1000, OverflowStrategy.dropNew)


    val factorial = Source(1 to 10).scan(1)(_ * _)

    implicit val dispatcher = system.dispatcher
    implicit val timeout = Timeout(5 seconds)

    val strings =  Source(1 to 10).mapAsync(4)(value => (toStringActor ? value).mapTo[String])

    val throttledAndZipped = Flow[String]
      .zip(factorial)
      .throttle(1, 1.second, 1, ThrottleMode.shaping)
      .mapAsync(10)(a=>Future(s"${new java.util.Date()} - $a"))
      .to(Sink.foreach(println))

    val toStringActorRef = throttledAndZipped.runWith(toStringActorPublisherSource)

    (1 to 10).map ( num => toStringActorRef !  s"<${num.toString}>")

    throttledAndZipped.runWith(strings)


    system.scheduler.scheduleOnce(12 second)(
      system.terminate()
    )




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
