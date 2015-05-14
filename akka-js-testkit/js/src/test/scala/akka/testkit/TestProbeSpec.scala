package akka.testkit

import language.postfixOps
import org.scalatest.WordSpec
import org.scalatest.Matchers
import org.scalatest.{ BeforeAndAfterEach, WordSpec }
import akka.actor._
import scala.concurrent.Future
import akka.concurrent.{ Await, BlockingEventLoop }
import scala.concurrent.duration._
import akka.pattern.ask
import scala.util.Try
import akka.concurrent.BlockingEventLoop
import scala.concurrent.duration._
import akka.concurrent.BlockingEventLoop

// @note IMPLEMENT IN SCALA.JS @org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class TestProbeSpec extends AkkaSpec with DefaultTimeout {

  "A TestProbe" must {

    "reply to futures" in {
      val tk = TestProbe() // TestProbe needs to happen before blocking mode kicks in, because it uses it itself (otherwise -> deadlock)
      BlockingEventLoop.switch
      val future = tk.ref ? "hello"
      tk.expectMsg(10 millis, "hello") // TestActor runs on CallingThreadDispatcher
      tk.lastMessage.sender ! "world"
      //future should be('completed)
      Await.result(future, timeout.duration) should be("world")
      BlockingEventLoop.reset
    }

    "reply to messages" in {
      val tk1 = TestProbe()
      val tk2 = TestProbe()
      BlockingEventLoop.switch
      tk1.ref.!("hello")(tk2.ref)
      tk1.expectMsg(10 millis, "hello")
      tk1.lastMessage.sender ! "world"
      tk2.expectMsg(10 millis, "world")
      BlockingEventLoop.reset
    }

    "properly send and reply to messages" in {
      val probe1 = TestProbe()
      val probe2 = TestProbe()
      BlockingEventLoop.switch
      probe1.send(probe2.ref, "hello")
      probe2.expectMsg(100 millis, "hello")
      probe2.lastMessage.sender ! "world"
      probe1.expectMsg(100 millis, "some hint here", "world")
      BlockingEventLoop.reset
    }

    def assertFailureMessageContains(expectedHint: String)(block: ⇒ Unit) {
      Try {
        block
      } match {
        case scala.util.Failure(e: AssertionError) ⇒
          if (!(e.getMessage contains expectedHint))
            fail(s"failure message did not contain hint! Was: ${e.getMessage}, expected to contain $expectedHint")
        case scala.util.Failure(oth) ⇒
          fail(s"expected AssertionError but got: $oth")
        case scala.util.Success(result) ⇒
          fail(s"expected failure but got: $result")
      }
    }

    "throw AssertionError containing hint in its message if max await time is exceeded" in {
      val probe = TestProbe()
      BlockingEventLoop.switch
      val hint = "some hint"

      assertFailureMessageContains(hint) {
        probe.expectMsg(10 millis, hint, "hello")
      }
      BlockingEventLoop.reset
    }

    "throw AssertionError containing hint in its message if received message doesn't match" in {
      val probe = TestProbe()
      BlockingEventLoop.switch
      val hint = "some hint"

      assertFailureMessageContains(hint) {
        probe.ref ! "hello"
        probe.expectMsg(10 millis, hint, "bye")
      }
      BlockingEventLoop.reset
    }

    "have an AutoPilot" in {
      //#autopilot
      val probe = TestProbe()
      BlockingEventLoop.switch
      probe.setAutoPilot(new TestActor.AutoPilot {
        def run(sender: ActorRef, msg: Any): TestActor.AutoPilot =
          msg match {
            case "stop" ⇒ TestActor.NoAutoPilot
            case x      ⇒ testActor.tell(x, sender); TestActor.KeepRunning
          }
      })
      //#autopilot
      probe.ref ! "hallo"
      probe.ref ! "welt"
      probe.ref ! "stop"
      expectMsg("hallo")
      expectMsg("welt")
      probe.expectMsg("hallo")
      probe.expectMsg("welt")
      probe.expectMsg("stop")
      probe.ref ! "hallo"
      probe.expectMsg("hallo")
      testActor ! "end"
      expectMsg("end") // verify that "hallo" did not get through
      BlockingEventLoop.reset
    }

    "be able to expect primitive types" in {
      BlockingEventLoop.switch
      // WHY 32768? Because class in Scala-js depends on the size of the number, not the instantiation type
      // If it were 42, class would be Byte

      for (_ ← 1 to 7) testActor ! 32768 
      expectMsgType[Int] should be(32768)
      expectMsgAnyClassOf(classOf[Int]) should be(32768)
      expectMsgAllClassOf(classOf[Int]) should be(Seq(32768))
      expectMsgAllConformingOf(classOf[Int]) should be(Seq(32768))
      expectMsgAllConformingOf(5 seconds, classOf[Int]) should be(Seq(32768))
      expectMsgAllClassOf(classOf[Int]) should be(Seq(32768))
      expectMsgAllClassOf(5 seconds, classOf[Int]) should be(Seq(32768))
      BlockingEventLoop.reset
    }

    "be able to ignore primitive types" in {
      BlockingEventLoop.switch
      ignoreMsg { case 42 ⇒ true }
      testActor ! 42
      testActor ! "pigdog"
      expectMsg("pigdog")
      BlockingEventLoop.reset
    }

    "watch actors when queue non-empty" in {
      val probe = TestProbe()
      // deadLetters does not send Terminated
      BlockingEventLoop.switch
      val target = system.actorOf(Props(new Actor {
        def receive = Actor.emptyBehavior
      }), "potatoActor")
      
      system.stop(target)  
      probe.ref ! "hello"
      probe watch target
      
      probe.expectMsg(1.seconds, "hello")
      probe.expectMsg(2.seconds, Terminated(target)(false, false))
      
      BlockingEventLoop.reset
    }

  }

}
