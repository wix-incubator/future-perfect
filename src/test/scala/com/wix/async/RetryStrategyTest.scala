package com.wix.async

import RetrySupport._
import java.io.IOException
import org.specs2.mutable.SpecificationWithJUnit

/**
 * @author shaiyallin
 * @since 12/24/12
 */

class RetryStrategyTest extends SpecificationWithJUnit {

  "onBusinessException" should {
    "return true" in {
      onCheckedException(new IOException) must beTrue
    }
    "return false" in {
      onCheckedException(new IllegalArgumentException) must beFalse
    }
  }

  "onException" should {
    "return true" in {
      onException(new IOException) must beTrue
      onException(new IllegalArgumentException) must beTrue
    }
    "return false" in {
      onException(new Error) must beFalse
    }
  }

  "onlyOnTimeout" should {
    "always return false" in {
      onlyOnTimeout(new IOException) must beFalse
      onlyOnTimeout(new IllegalArgumentException) must beFalse
    }
  }

  "on with varargs" should {
    val strategy = onAnyOf(classOf[IllegalArgumentException], classOf[ArrayIndexOutOfBoundsException])_
    "return true" in {
      strategy(new IllegalArgumentException) must beTrue
      strategy(new ArrayIndexOutOfBoundsException) must beTrue
    }
    "return false" in {
      strategy(new IOException) must beFalse
    }
  }

  "typed on" should {
    val strategy = on[IllegalArgumentException]_
    "return true" in {
      strategy(new IllegalArgumentException) must beTrue
    }
    "return false" in {
      strategy(new IOException) must beFalse
    }
  }

  "onceFor" should {
    val strategy = onceFor[IllegalArgumentException]
    "have one retry" in {
      strategy.retries must_== 1
    }
    "handle desired exception" in {
      strategy.shouldRetry(new IllegalArgumentException) must beTrue
    }
    "not handle other exception" in {
      strategy.shouldRetry(new IOException) must beFalse
    }
  }

}