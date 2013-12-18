package com.wix.async

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import org.mockito.Mockito._


/**
 * User: avitaln
 * Date: 9/30/13
 */
class RetrySupportTest extends SpecificationWithJUnit with Mockito {

  trait Dummy {
    def func() : Int
  }

  trait Context extends Scope with RetrySupport {
    val dummy = mock[Dummy]
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
