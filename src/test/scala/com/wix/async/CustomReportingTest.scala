package com.wix.async

import com.twitter.util.{CountDownLatch, Await}
import com.wix.async.FuturePerfect.{Successful, ExceededTimeout, Event}
import java.util.concurrent.{Executors, ScheduledExecutorService}
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import scala.concurrent.duration._

/**
 * User: orr
 * Date: 2/19/14
 */
class CustomReportingTest extends SpecificationWithJUnit with NoTimeConversions with Mockito {

  trait CustomReporting {
    def reportExceeded(result: String)
    def reportSuccessful(result: String)
  }

  trait StringReporting { this: Reporting[Event] =>
    def customReporting: CustomReporting

    listenFor {
      case ExceededTimeout(actual, name, result: String) => customReporting.reportExceeded(result)
      case Successful(actual, name, result: String) => customReporting.reportSuccessful(result)
    }
  }

  trait Context extends Scope with FuturePerfect with StringReporting {
    def executorService: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    val customReporting: CustomReporting = mock[CustomReporting]
    val latch = new CountDownLatch(1)
  }

  "an execution with custom reporting" should {
    "correctly report result for exceeded timeout event" in new Context {
      val result = "some result"
      val future = execution(timeout = 10 millis) {
        latch.await()
        result
      }

      Await.result(future) must throwA[TimeoutGaveUpException]
      latch.countDown()
      there was one(customReporting).reportExceeded(result)
    }

    "correctly report result for successful event" in new Context {
      val result = "some result"
      val future = execution {
        result
      }

      Await.result(future) must_== result
      there was one(customReporting).reportSuccessful(result)
    }
  }
}
