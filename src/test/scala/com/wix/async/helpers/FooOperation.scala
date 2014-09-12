package com.wix.async.helpers

import com.twitter.util.CountDownLatch

/**
 * User: nadavl
 * Date: 9/11/14
 * Time: 11:37 AM
 */
class FooOperation {
  private var times = 0
  private val latch = new CountDownLatch(1)
  def interactions: Int = times
  def succeed() = incAndThen(true)
  def await() = {
    latch.await()
    true
  }
  def release() {
    latch.countDown()
  }
  def explode(e: Throwable = new RuntimeException("**!big badaboom!**")): Boolean = incAndThen {
    throw e
  }
  def sleep(millis: Long) = incAndThen {
    Thread.sleep(millis)
    true
  }
  private var slept = 1
  def sleepDecreasing(millis: Long) = incAndThen {
    val duration = millis / slept
    slept = slept + 1
    Thread.sleep(duration)
    println("Slept for %s ms".format(duration))
    true
  }

  def explodeThenSucceed(succeedAfter: Int = 1) = incAndThen {
    if (times > succeedAfter)
      true
    else
      throw new RuntimeException("Kaboom!")
  }

  private def incAndThen[T](f: => T) = {
    times = times + 1
    f
  }
}
