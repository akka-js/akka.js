/**
  * Copyright (C) 2014-2016 Lightbend Inc. <http://www.lightbend.com>
  */
package akka.stream.impl

import scala.util.Random
import org.scalatest.{ MustMatchers, WordSpec }
import akka.stream.impl.ResizableMultiReaderRingBuffer._

class ResizableMultiReaderRingBufferSpec extends WordSpec with MustMatchers {

  "A ResizableMultiReaderRingBuffer" should {

    "initially be empty (1)" in new Test(iSize = 2, mSize = 4, cursorCount = 1) {
      inspect mustEqual "0 0 (size=0, writeIx=0, readIx=0, cursors=1)"
    }

    "initially be empty (2)" in new Test(iSize = 4, mSize = 4, cursorCount = 3) {
      inspect mustEqual "0 0 0 0 (size=0, writeIx=0, readIx=0, cursors=3)"
    }

    "fail reads if nothing can be read" in new Test(iSize = 4, mSize = 4, cursorCount = 3) {
      write(1) mustEqual true
      write(2) mustEqual true
      write(3) mustEqual true
      inspect mustEqual "1 2 3 0 (size=3, writeIx=3, readIx=0, cursors=3)"
      read(0) mustEqual 1
      read(0) mustEqual 2
      read(1) mustEqual 1
      inspect mustEqual "1 2 3 0 (size=3, writeIx=3, readIx=0, cursors=3)"
      read(0) mustEqual 3
      read(0) mustEqual null
      read(1) mustEqual 2
      inspect mustEqual "1 2 3 0 (size=3, writeIx=3, readIx=0, cursors=3)"
      read(2) mustEqual 1
      inspect mustEqual "0 2 3 0 (size=2, writeIx=3, readIx=1, cursors=3)"
      read(1) mustEqual 3
      read(1) mustEqual null
      read(2) mustEqual 2
      read(2) mustEqual 3
      inspect mustEqual "0 0 0 0 (size=0, writeIx=3, readIx=3, cursors=3)"
    }

    "fail writes if there is no more space" in new Test(iSize = 4, mSize = 4, cursorCount = 2) {
      write(1) mustEqual true
      write(2) mustEqual true
      write(3) mustEqual true
      write(4) mustEqual true
      write(5) mustEqual false
      read(0) mustEqual 1
      write(5) mustEqual false
      read(1) mustEqual 1
      write(5) mustEqual true
      read(0) mustEqual 2
      read(0) mustEqual 3
      read(0) mustEqual 4
      read(1) mustEqual 2
      write(6) mustEqual true
      inspect mustEqual "5 6 3 4 (size=4, writeIx=6, readIx=2, cursors=2)"
      read(0) mustEqual 5
      read(0) mustEqual 6
      read(0) mustEqual null
      read(1) mustEqual 3
      read(1) mustEqual 4
      read(1) mustEqual 5
      read(1) mustEqual 6
      read(1) mustEqual null
      inspect mustEqual "0 0 0 0 (size=0, writeIx=6, readIx=6, cursors=2)"
      write(7) mustEqual true
      write(8) mustEqual true
      write(9) mustEqual true
      inspect mustEqual "9 0 7 8 (size=3, writeIx=9, readIx=6, cursors=2)"
      read(0) mustEqual 7
      read(0) mustEqual 8
      read(0) mustEqual 9
      read(0) mustEqual null
      read(1) mustEqual 7
      read(1) mustEqual 8
      read(1) mustEqual 9
      read(1) mustEqual null
      inspect mustEqual "0 0 0 0 (size=0, writeIx=9, readIx=9, cursors=2)"
    }

    "automatically grow if possible" in new Test(iSize = 2, mSize = 8, cursorCount = 2) {
      write(1) mustEqual true
      inspect mustEqual "1 0 (size=1, writeIx=1, readIx=0, cursors=2)"
      write(2) mustEqual true
      inspect mustEqual "1 2 (size=2, writeIx=2, readIx=0, cursors=2)"
      write(3) mustEqual true
      inspect mustEqual "1 2 3 0 (size=3, writeIx=3, readIx=0, cursors=2)"
      write(4) mustEqual true
      inspect mustEqual "1 2 3 4 (size=4, writeIx=4, readIx=0, cursors=2)"
      read(0) mustEqual 1
      read(0) mustEqual 2
      read(0) mustEqual 3
      read(1) mustEqual 1
      read(1) mustEqual 2
      write(5) mustEqual true
      inspect mustEqual "5 0 3 4 (size=3, writeIx=5, readIx=2, cursors=2)"
      write(6) mustEqual true
      inspect mustEqual "5 6 3 4 (size=4, writeIx=6, readIx=2, cursors=2)"
      write(7) mustEqual true
      inspect mustEqual "3 4 5 6 7 0 0 0 (size=5, writeIx=5, readIx=0, cursors=2)"
      read(0) mustEqual 4
      read(0) mustEqual 5
      read(0) mustEqual 6
      read(0) mustEqual 7
      read(0) mustEqual null
      read(1) mustEqual 3
      read(1) mustEqual 4
      read(1) mustEqual 5
      read(1) mustEqual 6
      read(1) mustEqual 7
      read(1) mustEqual null
      inspect mustEqual "0 0 0 0 0 0 0 0 (size=0, writeIx=5, readIx=5, cursors=2)"
    }

    "pass the stress test" in {
      // create 100 buffers with an initialSize of 1 and a maxSize of 1 to 64,
      // for each one attach 1 to 8 cursors and randomly try reading and writing to the buffer;
      // in total 200 elements need to be written to the buffer and read in the correct order by each cursor
      val MAXSIZEBIT_LIMIT = 6 // 2 ^ (this number)
      val COUNTER_LIMIT = 200
      val LOG = false
      val sb = new java.lang.StringBuilder
      def log(s: ⇒ String): Unit = if (LOG) sb.append(s)

      class StressTestCursor(cursorNr: Int, run: Int) extends Cursor {
        var cursor: Int = _
        var counter = 1
        def tryReadAndReturnTrueIfDone(buf: TestBuffer): Boolean = {
          log(s"  Try reading of $toString: ")
          try {
            val x = buf.read(this)
            log("OK\n")
            if (x != counter)
              fail(s"""|Run $run, cursorNr $cursorNr, counter $counter: got unexpected $x
                       |  Buf: ${buf.inspect}
                       |  Cursors: ${buf.cursors.cursors.mkString("\n           ")}
                       |Log:\n$sb
                      """.stripMargin)
            counter += 1
            counter == COUNTER_LIMIT
          } catch {
            case NothingToReadException ⇒ log("FAILED\n"); false // ok, we currently can't read, try again later
          }
        }
        override def toString: String = s"cursorNr $cursorNr, ix $cursor, counter $counter"
      }

      val random = new Random
      for {
        bit ← 1 to MAXSIZEBIT_LIMIT
        n ← 1 to 2
      } {
        var counter = 1
        var activeCursors = List.tabulate(random.nextInt(8) + 1)(new StressTestCursor(_, 1 << bit))
        var stillWriting = 2 // give writing a slight bias, so as to somewhat "stretch" the buffer
        val buf = new TestBuffer(1, 1 << bit, new Cursors { def cursors = activeCursors })
        sb.setLength(0)
        while (activeCursors.nonEmpty) {
          log(s"Buf: ${buf.inspect}\n")
          val activeCursorCount = activeCursors.size
          val index = random.nextInt(activeCursorCount + stillWriting)
          if (index >= activeCursorCount) {
            log(s"  Writing $counter: ")
            if (buf.write(counter)) {
              log("OK\n")
              counter += 1
            } else {
              log("FAILED\n")
              if (counter == COUNTER_LIMIT) stillWriting = 0
            }
          } else {
            val cursor = activeCursors(index)
            if (cursor.tryReadAndReturnTrueIfDone(buf))
              activeCursors = activeCursors.filter(_ != cursor)
          }
        }
      }
    }
  }

  class TestBuffer(iSize: Int, mSize: Int, cursors: Cursors) extends ResizableMultiReaderRingBuffer[Int](iSize, mSize, cursors) {
    def inspect: String =
      underlyingArray.map(x ⇒ if (x == null) 0 else x).mkString("", " ", " " + toString.dropWhile(_ != '('))
  }

  class Test(iSize: Int, mSize: Int, cursorCount: Int) extends TestBuffer(iSize, mSize, new SimpleCursors(cursorCount)) {
    def read(cursorIx: Int): Integer =
      try read(cursors.cursors(cursorIx)) catch { case NothingToReadException ⇒ null }
  }

  class SimpleCursors(cursorCount: Int) extends Cursors {
    val cursors: List[Cursor] = List.fill(cursorCount)(new Cursor { var cursor: Int = _ })
  }
}
