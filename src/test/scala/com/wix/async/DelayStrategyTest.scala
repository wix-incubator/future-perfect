package com.wix.async

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.mock.Mockito
import org.specs2.time.NoTimeConversions
import scala.concurrent.duration._
/**
 * @author shaiyallin
 * @since 12/23/12
 */

class DelayStrategyTest extends SpecificationWithJUnit with Mockito with NoTimeConversions {

  "ExponentialDelay" should {
    "increase the retries" in {
      val retry = ExponentialDelay(1 second)
      val next = retry.next
      next.retryCount must_== 1
    }

    "sleep the appropriate amount of time" in {
      val mockSleep = mock[DelayStrategy.Sleeper]

      val retry = new ExponentialDelay(1 second) {
        override val sleep = mockSleep
      }
      retry.delay()
      there was one(mockSleep).apply(1000l)

      val next = retry.next
      next.delay()
      there was one(mockSleep).apply(2000l)

      val nextNext = next.next
      nextNext.delay()
      there was one(mockSleep).apply(4000l)

    }
  }
}