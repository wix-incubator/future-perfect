package com.wix.async

import RetrySupport._
import DelayStrategy._
import scala.reflect.ClassTag

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

object RetrySupport {

  type RetryStrategy = (Throwable => Boolean)

  val NoRetries = RetryPolicy()

  def onBusinessException(e: Throwable) = e.isInstanceOf[Exception] && !e.isInstanceOf[RuntimeException]
  def onException(e: Throwable) = e.isInstanceOf[Exception]
  def onlyOnTimeout(e: Throwable) = false
  def onAnyOf(types: Class[_ <: Throwable]*)(e: Throwable) = types.exists(_.isInstance(e))
  def on[E <: Throwable](e: Throwable)(implicit m: ClassTag[E]) = m.runtimeClass.isInstance(e)

  def onceFor[E <: Throwable](implicit m: ClassTag[E]) = RetryPolicy(retries = 1, shouldRetry = on[E])

}

case class RetryPolicy(
                        delayStrategy: DelayStrategy = NoDelay,
                        retries: Int = 0,
                        shouldRetry: RetryStrategy = RetrySupport.onException) {

  private[async] def shouldRetryFor(t: Throwable) = (
    (t.isInstanceOf[java.util.concurrent.TimeoutException]
      || t.isInstanceOf[com.twitter.util.TimeoutException]
      || shouldRetry(t))
      && retries > 0)

  private[async] def next = copy(delayStrategy = delayStrategy.next, retries = retries - 1)

}

