package com.wix

import scala.reflect.ClassTag

/**
 * @author shaiyallin
 * @since 12/19/13
 */
package object async {
  type RetryStrategy = (Throwable => Boolean)

  val NoRetries = RetryPolicy()

  def onCheckedException(e: Throwable) = e.isInstanceOf[Exception] && !e.isInstanceOf[RuntimeException]
  def onException(e: Throwable) = e.isInstanceOf[Exception]
  def onlyOnTimeout(e: Throwable) = false
  def onAnyOf(types: Class[_ <: Throwable]*)(e: Throwable) = types.exists(_.isInstance(e))
  def on[E <: Throwable](e: Throwable)(implicit m: ClassTag[E]) = m.runtimeClass.isInstance(e)

  def onceFor[E <: Throwable](implicit m: ClassTag[E]) = RetryPolicy(retries = 1, shouldRetry = on[E])


}