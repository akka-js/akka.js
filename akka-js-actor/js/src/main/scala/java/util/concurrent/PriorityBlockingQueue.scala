package java.util.concurrent

import java.util.PriorityQueue
import java.util.Comparator

class PriorityBlockingQueue[T](initialCapacity: Int, comparator: Comparator[_ >: T])
    extends PriorityQueue[T](initialCapacity, comparator) with BlockingQueue[T]
