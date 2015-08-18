package java.util.concurrent.locks

import java.util.concurrent.TimeUnit

class ReentrantLock {
  def lock() = ()
  def unlock() = ()
  def tryLock(timeout: Long, unit: TimeUnit): Boolean = true
}
