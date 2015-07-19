package com.wix.async

import com.wix.async.DelayStrategy._

import scala.annotation.tailrec

/**
 * User: avitaln
 * Date: 9/30/13
 */
trait RetrySupport {

  def withRetry[T](retryPolicy: RetryPolicy)(fn: => T): T = withRecursiveRetry[T](retryPolicy)(fn)

  @tailrec private def withRecursiveRetry[T](retryPolicy: RetryPolicy)(fn: => T): T =
    try {
      fn
    } catch {
      case t : Throwable =>
        if (retryPolicy.shouldRetryFor(t)) {
          retryPolicy.delayStrategy.delay()
          withRecursiveRetry(retryPolicy.next)(fn)
        }
        else throw t
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

