package akka.actor.test

import utest._
import utest.ExecutionContext.RunNow

object HelloTest extends TestSuite {
  val tests = TestSuite{
    'hello {
      'world {
        val x = 1
        val y = 2
        assert(x != y)
        (x, y)
      }
    }
    'test2 {
      val a = 1
      val b = 2
      assert(a == b)
    }
  }
}

