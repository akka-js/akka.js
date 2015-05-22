package raft

import org.scalatest._

class LogSpec extends WordSpecLike
    with MustMatchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  val testEntries = Vector(
    Entry("0", Term(0), Some(InternalClientRef(null, 0))), // 1
    Entry("1", Term(1), Some(InternalClientRef(null, 1))), // 2
    Entry("2", Term(2), Some(InternalClientRef(null, 2)))) // 3

  "a log" must {
    "append entries to the end of a log" in {
      val t2 = Vector(Entry("5", Term(5), Some(InternalClientRef(null, 5))))
      testEntries.append(t2) must be(testEntries ++ t2)
    }

    "append entries at a specific position" in {
      val t2 = Vector(Entry("5", Term(5), Some(InternalClientRef(null, 5))))

      testEntries.append(t2, 2) must be(Vector(
        Entry("0", Term(0), Some(InternalClientRef(null, 0))),
        Entry("1", Term(1), Some(InternalClientRef(null, 1))),
        Entry("5", Term(5), Some(InternalClientRef(null, 5)))))
    }

    "determine the term of an entry" in {
      testEntries.termOf(1) must be(Term(0))
      testEntries.termOf(2) must be(Term(1))
      testEntries.termOf(testEntries.length) must be(Term(2))
    }

    "element at position" in {
      testEntries.get(1) must be(Entry("0", Term(0), Some(InternalClientRef(null, 0))))
      testEntries.get(3) must be(Entry("2", Term(2), Some(InternalClientRef(null, 2))))
    }

    "last index is length" in {
      testEntries.lastIndex must be(3)
    }

    "last term is term of last element" in {
      testEntries.lastTerm must be(Term(2))
    }

  }
}