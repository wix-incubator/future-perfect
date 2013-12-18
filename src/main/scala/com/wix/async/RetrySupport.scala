package com.wix.async

import DelayStrategy._

/**
 * User: avitaln
 * Date: 9/30/13
 */
trait RetrySupport {

  def withRetry[T](retryPolicy: RetryPolicy)(fn: => T): T = {
    try {
      fn
    } catch {
      case e : Throwable if retryPolicy.shouldRetryFor(e) => withRetry(retryPolicy.next)(fn)
    }
  }
}

case class RetryPolicy(
                        delayStrategy: DelayStrategy = NoDelay,
                        retries: Int = 0,
                        shouldRetry: RetryStrategy = onException) {

  private[async] def shouldRetryFor(t: Throwable) = (
    (t.isInstanceOf[java.util.concurrent.TimeoutException]
      || t.isInstanceOf[com.twitter.util.TimeoutException]
      || shouldRetry(t))
      && retries > 0)

  private[async] def next = copy(delayStrategy = delayStrategy.next, retries = retries - 1)

}

