package com.wix.async

import scala.concurrent.duration.Duration

/**
 * @author shaiyallin
 * @since 12/23/12
 */

trait DelayStrategy {

  def next: DelayStrategy
  def delay()
}

object DelayStrategy {
  type Sleeper = (Long => Unit)
  val sleep: DelayStrategy.Sleeper = { millis =>
    Thread.sleep(millis)
  }

  // empty, default implementation - no sleep
  object NoDelay extends DelayStrategy {
     def next = this

     def delay() {
       // noop
     }
  }

  def constant(duration: Duration) = ConstantDelay(duration)
  def exponential(initialDuration: Duration) = ExponentialDelay(initialDuration)
}

private[async] case class ConstantDelay(duration: Duration, sleep: DelayStrategy.Sleeper = DelayStrategy.sleep) extends DelayStrategy {
  def next = ConstantDelay(duration, sleep)
  def delay() {
    sleep(duration.toMillis)
  }
}

private[async] case class ExponentialDelay(initialDuration: Duration, retryCount: Int = 0,
                            sleep: DelayStrategy.Sleeper = DelayStrategy.sleep) extends DelayStrategy {
  def next = ExponentialDelay(initialDuration, retryCount + 1, sleep)
  def delay() {
    sleep(initialDuration.toMillis * math.pow(2, retryCount).toLong)
  }
}
