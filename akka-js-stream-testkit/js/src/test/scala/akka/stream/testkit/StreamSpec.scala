/**
 * Copyright (C) 2015-2016 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.testkit

import akka.actor.{ ActorSystem, ActorRef }
import akka.stream.impl.StreamSupervisor
import akka.stream.snapshot.{ MaterializerState, StreamSnapshotImpl }
import akka.testkit.{ AkkaSpec, TestProbe }
import com.typesafe.config.{ ConfigFactory, Config }
import org.scalatest.Failed

import scala.concurrent.Future
import scala.concurrent.duration._

class StreamSpec(_system: ActorSystem) extends AkkaSpec(_system) {
  def this(config: Config) =
    this(ActorSystem("streamspec", config.withFallback(AkkaSpec.testConf)))
      //AkkaSpec.getCallerName(getClass),
      //ConfigFactory.load(config.withFallback(AkkaSpec.testConf))))

  def this(s: String) = this(ConfigFactory.parseString(s))

  //def this(configMap: Map[String, _]) = this(ActorSystem(AkkaSpec.testConf))
  //this(AkkaSpec.mapToConfig(configMap))

  def this() =
    this(ConfigFactory.empty())
    //this(ActorSystem(AkkaSpec.getCallerName(getClass), AkkaSpec.testConf))

  override def withFixture(test: NoArgTest) = {
    super.withFixture(test) match {
      case failed: Failed ⇒
        implicit val ec = system.dispatcher
        val probe = TestProbe()(system)
        system.actorSelection("/user/" + StreamSupervisor.baseName + "*").tell(StreamSupervisor.GetChildren, probe.ref)
        val children: Seq[ActorRef] = probe.receiveWhile(2.seconds) {
          case StreamSupervisor.Children(children) ⇒ children
        }.flatten
        println("--- Stream actors debug dump ---")
        if (children.isEmpty) println("Stream is completed. No debug information is available")
        else {
          println("Stream actors alive: " + children)
          // children.foreach(_ ! StreamSupervisor.PrintDebugDump)
          Future
            .sequence(children.map(MaterializerState.requestFromChild))
            .foreach(snapshots =>
              snapshots.foreach(s =>
                akka.stream.testkit.scaladsl.StreamTestKit.snapshotString(s.asInstanceOf[StreamSnapshotImpl])))
        }
        failed
      case other ⇒ other
    }
  }
}
