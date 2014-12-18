package com.wix.async

import java.util.concurrent.{TimeUnit, Executors, TimeoutException}
import scala.concurrent._
import com.wix.async.FuturePerfect._
import com.wix.async.helpers.{FooOperation, TestableDelayStrategy}
import org.jmock.lib.concurrent.DeterministicScheduler
import org.specs2.matcher._
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._

/**
 * @author shaiyallin
 * @since 11/5/12
 */

class FuturePerfectTest extends SpecificationWithJUnit with Mockito with NoTimeConversions with MatcherMacros {

  sequential

  "Future Perfect" should {
    // this test is here to prevent someone from breaking async execution by causing everything to execute from the calling thread
    "execute in a background thread and not from the calling thread" in new Context {

      var asyncThreadId: Long = 0
      waitFor {
        execution {
          asyncThreadId = Thread.currentThread().getId
        }
      }
      Thread.currentThread().getId must_!= asyncThreadId
    }

    "succeed when blocking function succeeds" in new Context {
      val results = waitFor {
        execution {
          bar.succeed()
        }
      }
      results must beTrue
    }

    "report success duration with default execution name" in new Context {
      waitFor {
        execution(timeout) { /* do nothing on purpose */ }
      }
      there was one(reporter).report(matchA[Successful].executionName("async"))
    }


    "report success duration with custom execution name" in new Context {
      waitFor {
        execution(timeout = timeout, name = "foo") { /* do nothing on purpose */ }
      }
      there was one(reporter).report(matchA[Successful].executionName("foo"))
    }

    "report time spent in queue" in new Context {
      waitFor{
        execution(timeout) { /* do nothing on purpose */ }
      }
      there was one(reporter).report(matchA[TimeSpentInQueue])
    }

    "report when timed out while in queue" in new Context {
      override val executorService = new DeterministicScheduler

      val f = execution(timeout) { /* do nothing on purpose */}

      theExecutionOf(f) must throwA[TimeoutGaveUpException]
      there was one(reporter).report(matchA[TimeoutWhileInQueue])

      executorService.runUntilIdle()
    }.pendingUntilFixed("future.recoverWith runs with the same executor as the test; when it's a deterministc executor, it doesn't run at all, thus fucking up our test")

    "fail when blocking function fails" in new Context {
      val error = new RuntimeException("Kaboom!")
      val f = execution {
        bar.explode(error)
      }

      theExecutionOf(f) must throwAn(error)
      there was one(reporter).report(matchA[Failed].error(error))
    }

    "timeout when blocking function stalls" in new Context {
      val f = execution(timeout = timeout) {
        bar.await()
      }
      theExecutionOf(f) must throwA[TimeoutGaveUpException]

      bar.release()
      there was one(reporter).report(matchA[ExceededTimeout].result(true))
      there was one(reporter).report(matchA[GaveUp])
    }

    "retry on timeout" in new Context {
      waitFor{
        execution(timeout, RetryPolicy(retries = 1)) {
          bar.sleepDecreasing(150)
        }
      }
      bar.interactions must_== 2

      there was one(reporter).report(matchA[Retrying].remainingRetries(1))
    }

    "retry on expected error" in new Context {
      waitFor {
        execution(RetryPolicy(
          retries = 1,
          shouldRetry = _.isInstanceOf[RuntimeException])) {
          bar.explodeThenSucceed()
        }
      }
      bar.interactions must_== 2
      there was one(reporter).report(matchA[Retrying].remainingRetries(1))
    }

    "delay before retrying" in new Context {
      val delayStrategy = TestableDelayStrategy()
      waitFor {
        execution(RetryPolicy(retries = 3, shouldRetry = (e => true), delayStrategy = delayStrategy)) {
          bar.explodeThenSucceed(succeedAfter = 2)
        }
      }
      there was two(delayStrategy).delay()
    }

    "give up after retrying up to the limit" in new Context {
      var f = execution(RetryPolicy( retries = 1, shouldRetry = _.isInstanceOf[RuntimeException])) {
        bar.explode(new RuntimeException("Kaboom!"))
      }
      
      theExecutionOf(f) must throwA[RuntimeException]
      bar.interactions must_== 2
    }

    "not retry on unexpected error" in new Context {
      val f = execution(RetryPolicy(retries = 2, shouldRetry = _.isInstanceOf[IllegalStateException])) {
        bar.explode(new RuntimeException("kaboom!"))
      }
      theExecutionOf(f) must throwA[RuntimeException]
      bar.interactions must_== 1
    }

    "wrap timeout exceptions" in new Context {
      val f = execution(timeout, NoRetries, { case e: TimeoutException => new CustomException(e)}) {
        bar.await()
      }
      theExecutionOf(f) must throwA[CustomException]
      bar.release()
    }

    "wrap timeout exception after retry" in new Context {
      val f = execution(timeout = timeout, retryPolicy = RetryPolicy(retries = 1), onTimeout = {
        case e: TimeoutException => new CustomException(e)
      }) {
        bar.await()
      }

      theExecutionOf(f) must throwA[CustomException]
      bar.release()
    }

    "convert automatically to Scala future" in new Context {
      import com.wix.async.Implicits._
      import com.twitter.{ util => tw}

      tw.Await.result(execution {true}, tw.Duration(100, TimeUnit.MILLISECONDS)) must beTrue
    }

  }

  class CustomException(cause: Throwable) extends RuntimeException(cause)

  class Context extends Scope with FuturePerfect {
    val reporter = mock[Reporter[Event]]
    val executorService = Executors.newFixedThreadPool(4)
    register(reporter)
    val bar = new FooOperation
    val timeout = 100.millis
    val atMost = 1.second

    def theExecutionOf[T] = waitFor[T] _

    def waitFor[T](future: Future[T]): T = {
      Await.result(future, atMost)
    }
  }

}


