package com.wix.async

import org.mockito.Mockito._
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions

import scala.compat.Platform
import scala.concurrent.duration._


/**
 * User: avitaln
 * Date: 9/30/13
 */
class RetrySupportTest extends SpecificationWithJUnit with Mockito with NoTimeConversions {

  trait Dummy {
    def func() : Int
    def elapsedFromSpecStart(): Duration
  }

  trait Context extends Scope with RetrySupport {
    val dummy = mock[Dummy]
    val startTime = Platform.currentTime
    val elapsedTimeAnswer = new MockAnswer[Duration]({ _ => elapsedUntilNow })
    val retryDelay: Duration = 200.millis

    def elapsedUntilNow = (Platform.currentTime - startTime).millis
  }

  "withRetry" should {
    "retry if expected exception is thrown and return the underlying return value" in new Context {
      when(dummy.func()).thenThrow(new IllegalArgumentException).thenReturn(88)

      val result = withRetry(onceFor[IllegalArgumentException]) {
        dummy.func()
      }

      result must_== 88

      there were two(dummy).func()
    }

    "delay retries according to the specified delay strategy" in new Context {
      when(dummy.elapsedFromSpecStart())
        .thenThrow(new IllegalArgumentException)
        .thenAnswer(elapsedTimeAnswer)

      val retryPolicyWithDelay = onceFor[IllegalArgumentException].copy(
        delayStrategy = DelayStrategy.constant(retryDelay)
      )

      val elapsedUntilSecondRetry = withRetry(retryPolicyWithDelay) {
        dummy.elapsedFromSpecStart()
      }

      elapsedUntilSecondRetry must {
        beGreaterThanOrEqualTo(retryDelay) and
        beLessThanOrEqualTo(elapsedUntilNow)
      }

      there were two(dummy).elapsedFromSpecStart()
    }

    "not retry on another exception" in new Context {
      when(dummy.func()).thenThrow(new IllegalStateException()).thenReturn(88)

      withRetry(onceFor[IllegalArgumentException]) {
        dummy.func()
      } must throwA[IllegalStateException]

      there was one(dummy).func()
    }

    "not retry if no exception" in new Context {
      when(dummy.func()).thenReturn(88)

      val result = withRetry(onceFor[IllegalArgumentException]) {
        dummy.func()
      }

      result must_== 88

      there was one(dummy).func()
    }

  }

}
