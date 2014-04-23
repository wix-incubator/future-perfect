package com.wix.async

import com.twitter.util.CountDownLatch
import com.wix.async.FuturePerfect.{Successful, ExceededTimeout, Event}
import java.util.concurrent.{ExecutorService, Executors}
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

  "an execution with custom reporting" should {
    "correctly report result for exceeded timeout event by passing the calculated value to the reporter" in new Context {
      val future = execution(timeout = 1 millis) {
        latch.await()
        result
      }.onFailure {
        case e: TimeoutGaveUpException => latch.countDown()
      }

      waitForExceededReport
      there was one(customReporting).reportExceeded(result)
    }

    "correctly report result for successful event by passing the calculated value to the reporter" in new Context {
      val future = execution {
        result
      }

      waitForSuccessfulReport
      there was one(customReporting).reportSuccessful(result)
    }
  }

  trait Context extends Scope with FuturePerfect with StringReporting {
    def executorService: ExecutorService = Executors.newFixedThreadPool(4)
    val customReporting: CustomReporting = mock[CustomReporting]
        customReporting.reportExceeded(any) answers {_ => customReportingExceededLatch.countDown()}
        customReporting.reportSuccessful(any) answers {_ => customReportingSuccessLatch.countDown()}

    val latch = new CountDownLatch(1)
    private val customReportingExceededLatch = new CountDownLatch(1)
    private val customReportingSuccessLatch = new CountDownLatch(1)

    def waitForExceededReport = customReportingExceededLatch.await()
    def waitForSuccessfulReport = customReportingSuccessLatch.await()

    val result = "some result"
  }

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
}
