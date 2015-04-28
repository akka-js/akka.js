/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.event

import akka.actor.ActorRef
/** @note IMPLEMENT IN SCALA.JS
import akka.util.Index
import java.util.concurrent.ConcurrentSkipListSet
*/
import java.util.Comparator
import akka.util.{ Subclassification, SubclassifiedIndex }
import scala.collection.mutable.TreeSet
import scala.collection.mutable
import scala.collection.immutable

/**
 * Represents the base type for EventBuses
 * Internally has an Event type, a Classifier type and a Subscriber type
 *
 * For the Java API, see akka.event.japi.*
 */
trait EventBus {
  type Event
  type Classifier
  type Subscriber

  //#event-bus-api
  /**
   * Attempts to register the subscriber to the specified Classifier
   * @return true if successful and false if not (because it was already
   *   subscribed to that Classifier, or otherwise)
   */
  def subscribe(subscriber: Subscriber, to: Classifier): Boolean

  /**
   * Attempts to deregister the subscriber from the specified Classifier
   * @return true if successful and false if not (because it wasn't subscribed
   *   to that Classifier, or otherwise)
   */
  def unsubscribe(subscriber: Subscriber, from: Classifier): Boolean

  /**
   * Attempts to deregister the subscriber from all Classifiers it may be subscribed to
   */
  def unsubscribe(subscriber: Subscriber): Unit

  /**
   * Publishes the specified Event to this bus
   */
  def publish(event: Event): Unit
  //#event-bus-api
}

/**
 * Represents an EventBus where the Subscriber type is ActorRef
 */
trait ActorEventBus extends EventBus {
  type Subscriber = ActorRef
  protected def compareSubscribers(a: ActorRef, b: ActorRef) = a compareTo b
}

/**
 * Can be mixed into an EventBus to specify that the Classifier type is ActorRef
 */
trait ActorClassifier { this: EventBus ⇒
  type Classifier = ActorRef
}

/**
 * Can be mixed into an EventBus to specify that the Classifier type is a Function from Event to Boolean (predicate)
 */
trait PredicateClassifier { this: EventBus ⇒
  type Classifier = Event ⇒ Boolean
}

/**
 * Maps Subscribers to Classifiers using equality on Classifier to store a Set of Subscribers (hence the need for compareSubscribers)
 * Maps Events to Classifiers through the classify-method (so it knows who to publish to)
 *
 * The compareSubscribers need to provide a total ordering of the Subscribers
 */
trait LookupClassification { this: EventBus ⇒

  /** @note IMPLEMENT IN SCALA.JS
  protected final val subscribers = new Index[Classifier, Subscriber](mapSize(), new Comparator[Subscriber] {
    def compare(a: Subscriber, b: Subscriber): Int = compareSubscribers(a, b)
  })
  */
  protected final var subscribers = new mutable.HashMap[Classifier, mutable.Set[Subscriber]] 
                                      with mutable.MultiMap[Classifier, Subscriber]

  /**
   * This is a size hint for the number of Classifiers you expect to have (use powers of 2)
   */
  protected def mapSize(): Int

  /**
   * Provides a total ordering of Subscribers (think java.util.Comparator.compare)
   */
  protected def compareSubscribers(a: Subscriber, b: Subscriber): Int

  /**
   * Returns the Classifier associated with the given Event
   */
  protected def classify(event: Event): Classifier

  /**
   * Publishes the given Event to the given Subscriber
   */
  protected def publish(event: Event, subscriber: Subscriber): Unit

  /** @note IMPLEMENT IN SCALA.JS
  def subscribe(subscriber: Subscriber, to: Classifier): Boolean = subscribers.put(to, subscriber)

  def unsubscribe(subscriber: Subscriber, from: Classifier): Boolean = subscribers.remove(from, subscriber)

  def unsubscribe(subscriber: Subscriber): Unit = subscribers.removeValue(subscriber)

  def publish(event: Event): Unit = {
    val i = subscribers.valueIterator(classify(event))
    while (i.hasNext) publish(event, i.next())
  }
  */
  
  def subscribe(subscriber: Subscriber, to: Classifier): Boolean = {
    subscribers.addBinding(to, subscriber)
    true
  }

  def unsubscribe(subscriber: Subscriber, from: Classifier): Boolean = {
    subscribers.removeBinding(from, subscriber)
    true
  }

  def unsubscribe(subscriber: Subscriber): Unit = {
    val it = subscribers.toIterator
    while(it.hasNext) {
      val (k, vs) = it.next()
      
      vs -= subscriber
      if(vs.isEmpty) subscribers -= k
    }
  }

  def publish(event: Event): Unit = {
    subscribers.get(classify(event)) match {
      case Some(set) ⇒ set.map(publish(event, _))   
    }
  }
}

/**
 * Classification which respects relationships between channels: subscribing
 * to one channel automatically and idempotently subscribes to all sub-channels.
 */
trait SubchannelClassification { this: EventBus ⇒

  /**
   * The logic to form sub-class hierarchy
   */
  protected implicit def subclassification: Subclassification[Classifier]

  // must be lazy to avoid initialization order problem with subclassification
  private lazy val subscriptions = new SubclassifiedIndex[Classifier, Subscriber]()

  @volatile
  private var cache = Map.empty[Classifier, Set[Subscriber]]

  /**
   * Returns the Classifier associated with the given Event
   */
  protected def classify(event: Event): Classifier

  /**
   * Publishes the given Event to the given Subscriber
   */
  protected def publish(event: Event, subscriber: Subscriber): Unit

  def subscribe(subscriber: Subscriber, to: Classifier): Boolean = subscriptions.synchronized {
    val diff = subscriptions.addValue(to, subscriber)
    addToCache(diff)
    diff.nonEmpty
  }

  def unsubscribe(subscriber: Subscriber, from: Classifier): Boolean = subscriptions.synchronized {
    val diff = subscriptions.removeValue(from, subscriber)
    // removeValue(K, V) does not return the diff to remove from or add to the cache
    // but instead the whole set of keys and values that should be updated in the cache
    cache ++= diff
    diff.nonEmpty
  }

  def unsubscribe(subscriber: Subscriber): Unit = subscriptions.synchronized {
    removeFromCache(subscriptions.removeValue(subscriber))
  }

  def publish(event: Event): Unit = {
    val c = classify(event)
    val recv =
      if (cache contains c) cache(c) // c will never be removed from cache
      else subscriptions.synchronized {
        if (cache contains c) cache(c)
        else {
          addToCache(subscriptions.addKey(c))
          cache(c)
        }
      }
    recv foreach (publish(event, _))
  }

  private def removeFromCache(changes: immutable.Seq[(Classifier, Set[Subscriber])]): Unit =
    cache = (cache /: changes) {
      case (m, (c, cs)) ⇒ m.updated(c, m.getOrElse(c, Set.empty[Subscriber]) -- cs)
    }

  private def addToCache(changes: immutable.Seq[(Classifier, Set[Subscriber])]): Unit =
    cache = (cache /: changes) {
      case (m, (c, cs)) ⇒ m.updated(c, m.getOrElse(c, Set.empty[Subscriber]) ++ cs)
    }
}

/**
 * Maps Classifiers to Subscribers and selects which Subscriber should receive which publication through scanning through all Subscribers
 * through the matches(classifier, event) method
 *
 * Note: the compareClassifiers and compareSubscribers must together form an absolute ordering (think java.util.Comparator.compare)
 */
trait ScanningClassification { self: EventBus ⇒
  /** @note IMPLEMENT IN SCALA.JS
  protected final val subscribers = new ConcurrentSkipListSet[(Classifier, Subscriber)](new Comparator[(Classifier, Subscriber)] {
    def compare(a: (Classifier, Subscriber), b: (Classifier, Subscriber)): Int = compareClassifiers(a._1, b._1) match {
      case 0     ⇒ compareSubscribers(a._2, b._2)
      case other ⇒ other
    }
  })
  */
  import scala.math.Ordering
  implicit val ordering = new Ordering[(Classifier, Subscriber)] {
    def compare(a: (Classifier, Subscriber), b: (Classifier, Subscriber)): Int = compareClassifiers(a._1, b._1) match {
      case 0     ⇒ compareSubscribers(a._2, b._2)
      case other ⇒ other
    }
  }
  protected final val subscribers = new TreeSet[(Classifier, Subscriber)]

  /**
   * Provides a total ordering of Classifiers (think java.util.Comparator.compare)
   */
  protected def compareClassifiers(a: Classifier, b: Classifier): Int

  /**
   * Provides a total ordering of Subscribers (think java.util.Comparator.compare)
   */
  protected def compareSubscribers(a: Subscriber, b: Subscriber): Int

  /**
   * Returns whether the specified Classifier matches the specified Event
   */
  protected def matches(classifier: Classifier, event: Event): Boolean

  /**
   * Publishes the specified Event to the specified Subscriber
   */
  protected def publish(event: Event, subscriber: Subscriber): Unit

  def subscribe(subscriber: Subscriber, to: Classifier): Boolean = subscribers.add((to, subscriber))

  def unsubscribe(subscriber: Subscriber, from: Classifier): Boolean = subscribers.remove((from, subscriber))

  def unsubscribe(subscriber: Subscriber): Unit = {
    val i = subscribers.iterator
    while (i.hasNext) {
      val e = i.next()
      // @note IMPLEMENT IN SCALA.JS if (compareSubscribers(subscriber, e._2) == 0) i.remove()
      if (compareSubscribers(subscriber, e._2) == 0) subscribers -= e
    }
  }

  def publish(event: Event): Unit = {
    val currentSubscribers = subscribers.iterator
    while (currentSubscribers.hasNext) {
      val (classifier, subscriber) = currentSubscribers.next()
      if (matches(classifier, event))
        publish(event, subscriber)
    }
  }
}

/**
 * Maps ActorRefs to ActorRefs to form an EventBus where ActorRefs can listen to other ActorRefs
 */
trait ActorClassification { this: ActorEventBus with ActorClassifier ⇒
  /** @note IMPLEMENT IN SCALA.JS import java.util.concurrent.ConcurrentHashMap */
  import scala.annotation.tailrec
  import scala.collection.mutable.HashMap
  private val empty = TreeSet.empty[ActorRef]
  // @note IMPLEMENT IN SCALA.JS private val mappings = new ConcurrentHashMap[ActorRef, TreeSet[ActorRef]](mapSize)
  private val mappings = new HashMap[ActorRef, TreeSet[ActorRef]]
  // we need this because HashMap doesn't provide `replace`, eventually we should move this into its own trait
  private def hashMapReplace(key: ActorRef, oldValue: TreeSet[ActorRef], newValue: TreeSet[ActorRef]) = {
    if(mappings.contains(key) && (mappings(key) eq oldValue)) {
      mappings(key) = newValue
      true
    }
    false
  } 
  private def hashMapRemove(key: ActorRef, value: TreeSet[ActorRef]) = {
    if(mappings.contains(key) && (mappings(key) eq value)) {
      mappings -= key
      true
    }
    false
  }

  @tailrec
  protected final def associate(monitored: ActorRef, monitor: ActorRef): Boolean = {
    val current = mappings get monitored
    current match {
      // @note IMPLEMENT IN SCALA.JS case null ⇒
      case None ⇒
        if (monitored.isTerminated) false
        else {
          /** @note IMPLEMENT IN SCALA.JS 
          if (mappings.putIfAbsent(monitored, empty + monitor) ne null) { 
          */
          if (!(mappings contains monitored)) {
            mappings += monitored -> (empty + monitor)
            associate(monitored, monitor) 
          }
          else if (monitored.isTerminated) !dissociate(monitored, monitor) else true
        }
      // @note IMPLEMENT IN SCALA.JS case raw: TreeSet[_] ⇒
      case Some(raw) ⇒
        val v = raw.asInstanceOf[TreeSet[ActorRef]]
        if (monitored.isTerminated) false
        if (v.contains(monitor)) true
        else {
          val added = v + monitor
          // @note IMPLEMENT IN SCALA.JS if (!mappings.replace(monitored, v, added)) associate(monitored, monitor)
          if (!hashMapReplace(monitored, v, added)) { 
            associate(monitored, monitor) 
          }
          else if (monitored.isTerminated) !dissociate(monitored, monitor) else true
        }
    }
  }

  protected final def dissociate(monitored: ActorRef): mutable.Iterable[ActorRef] = {
    @tailrec
    def dissociateAsMonitored(monitored: ActorRef): mutable.Iterable[ActorRef] = {
      val current = mappings get monitored
      current match {
        // @note IMPLEMENT IN SCALA.JS case null ⇒
        case None ⇒ empty
        // @note IMPLEMENT IN SCALA.JS case raw: TreeSet[_] ⇒
        case Some(raw) ⇒
          val v = raw.asInstanceOf[TreeSet[ActorRef]]
          //@note IMPLEMENT IN SCALA.JS if (!mappings.remove(monitored, v)) dissociateAsMonitored(monitored)
          if (!hashMapRemove(monitored, v)) dissociateAsMonitored(monitored)
          else v
      }
    }

    def dissociateAsMonitor(monitor: ActorRef): Unit = {
      // @note IMPLEMENT IN SCALA.JS val i = mappings.entrySet.iterator
       val i = mappings.iterator
      while (i.hasNext) {
        val entry = i.next
        // @note IMPLEMENT IN SCALA.JS val v = entry.getValue()
        val v = entry._2
        v match {
          case raw: TreeSet[_] ⇒
            val monitors = raw.asInstanceOf[TreeSet[ActorRef]]
            if (monitors.contains(monitor))
              // @note IMPLEMENT IN SCALA.JS dissociate(entry.getKey(), monitor)
              dissociate(entry._1, monitor)
          case _ ⇒ //Dun care
        }
      }
    }

    try { dissociateAsMonitored(monitored) } finally { dissociateAsMonitor(monitored) }
  }

  @tailrec
  protected final def dissociate(monitored: ActorRef, monitor: ActorRef): Boolean = {
    val current = mappings get monitored
    current match {
      // @note IMPLEMENT IN SCALA.JS case null ⇒
      case None ⇒ false
      // @note IMPLEMENT IN SCALA.JS case raw: TreeSet[_] ⇒
      case Some(raw) ⇒
        val v = raw.asInstanceOf[TreeSet[ActorRef]]
        val removed = v - monitor
        if (removed eq raw) false
        else if (removed.isEmpty) {
          //@note IMPLEMENT IN SCALA.JS if (!mappings.remove(monitored, v)) dissociate(monitored, monitor) else true
          if (!hashMapRemove(monitored, v)) dissociate(monitored, monitor) else true
        } else {
          //@note IMPLEMENT IN SCALA.JS if (!mappings.replace(monitored, v, removed)) dissociate(monitored, monitor) else true
          if (!hashMapReplace(monitored, v, removed)) dissociate(monitored, monitor) else true
        }
    }
  }

  /**
   * Returns the Classifier associated with the specified Event
   */
  protected def classify(event: Event): Classifier

  /**
   * This is a size hint for the number of Classifiers you expect to have (use powers of 2)
   */
  protected def mapSize: Int

  def publish(event: Event): Unit = mappings.get(classify(event)) match {
    /** @note IMPLEMENT IN SCALA.JS 
    case null ⇒ ()
    case some ⇒ some foreach { _ ! event }
    */
    case None ⇒ ()
    case Some(some) ⇒ some foreach { _ ! event }
  }

  def subscribe(subscriber: Subscriber, to: Classifier): Boolean =
    if (subscriber eq null) throw new IllegalArgumentException("Subscriber is null")
    else if (to eq null) throw new IllegalArgumentException("Classifier is null")
    else associate(to, subscriber)

  def unsubscribe(subscriber: Subscriber, from: Classifier): Boolean =
    if (subscriber eq null) throw new IllegalArgumentException("Subscriber is null")
    else if (from eq null) throw new IllegalArgumentException("Classifier is null")
    else dissociate(from, subscriber)

  def unsubscribe(subscriber: Subscriber): Unit =
    if (subscriber eq null) throw new IllegalArgumentException("Subscriber is null")
    else dissociate(subscriber)
}
