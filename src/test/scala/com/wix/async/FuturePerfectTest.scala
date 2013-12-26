package com.wix.async

import java.util.concurrent.Executors
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.time.NoTimeConversions
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import FuturePerfect._
import org.specs2.matcher._
import org.jmock.lib.concurrent.DeterministicScheduler
import com.twitter.util.{CountDownLatch, TimeoutException, Await}

/**
 * @author shaiyallin
 * @since 11/5/12
 */

class FuturePerfectTest extends SpecificationWithJUnit with Mockito with NoTimeConversions with MatcherMacros {

  sequential

  "Future Perfect" should {
    // this test is here to prevent someone from breaking async execution by causing everything to execute from the calling thread
    "execute in a background thread and not from the calling thread" in new AsyncScope {

      var asyncThreadId: Long = 0
      val f = execution {
        asyncThreadId = Thread.currentThread().getId
      }

      Thread.currentThread().getId must_!= asyncThreadId
    }
    
    "succeed when blocking function succeeds" in new AsyncScope {
      val f = execution {
        bar.succeed()
      }
      Await.result(f) must beTrue
    }

    "report success duration with default execution name" in new AsyncScope {
      Await.result(execution(timeout) { /* do nothing on purpose */ })

      there was one(reporter).report(matchA[Successful].executionName("async"))
    }


    "report success duration with custom execution name" in new AsyncScope {
      Await.result(execution(timeout = timeout, name = "foo") { /* do nothing on purpose */ })

      there was one(reporter).report(matchA[Successful].executionName("foo"))
    }

    "report time spent in queue" in new AsyncScope {
      Await.result(execution(timeout) { /* do nothing on purpose */ })

      there was one(reporter).report(matchA[TimeSpentInQueue])
    }

    "report when timed out while in queue" in new AsyncScope {

      override val executorService = new DeterministicScheduler
      override implicit lazy val timer = new ScheduledExecutorServiceTimer(Executors.newScheduledThreadPool(10))

      val f = execution(timeout) { /* do nothing on purpose */ }

      Await.result(f) must throwA[TimeoutGaveUpException]
      there was one(reporter).report(matchA[TimeoutWhileInQueue])

      executorService.runUntilIdle()
    }

    "fail when blocking function fails" in new AsyncScope {
      val error = new RuntimeException("Kaboom!")
      val f = execution {
        bar.explode(error)
      }
      Await.result(f) must throwA(error)

      there was one(reporter).report(matchA[Failed].error(error))
    }

    "timeout when blocking function stalls" in new AsyncScope {

      val f = execution(timeout = timeout) {
        bar.await()
      }
      Await.result(f) must throwA[TimeoutGaveUpException]
      bar.release()

      there was one(reporter).report(matchA[ExceededTimeout])
      there was one(reporter).report(matchA[GaveUp])
    }

    "retry on timeout" in new AsyncScope {
      val f = execution(timeout, RetryPolicy(retries = 1)) {
        bar.sleepDecreasing(150)
      }
      Await.result(f) must beTrue
      bar.times must_== 2

      there was one(reporter).report(matchA[Retrying].remainingRetries(1))
    }

    "retry on expected error" in new AsyncScope {
      val f = execution(RetryPolicy(
        retries = 1,
        shouldRetry = (e => e.isInstanceOf[RuntimeException]))) {
        bar.explodeThenSucceed()
      }
      Await.result(f) must beTrue
      bar.times must_== 2

      there was one(reporter).report(matchA[Retrying].remainingRetries(1))
    }

    "give up after retrying up to the limit" in new AsyncScope {
      val f = execution(RetryPolicy(
        retries = 1,
        shouldRetry = e => e.isInstanceOf[RuntimeException]
      )) {
        bar.explode(new RuntimeException("Kaboom!"))
      }
      Await.result(f) must throwA[RuntimeException]
      bar.times must_== 2
    }

    "not retry on unexpected error" in new AsyncScope {
      val f = execution(RetryPolicy(retries = 2, shouldRetry = (e => e.isInstanceOf[IllegalStateException]))) {
        println("Trying")
        bar.explode(new RuntimeException("Kaboom!"))
      }
      Await.result(f) must throwA[RuntimeException]
      bar.times must_== 1
    }

    "wrap timeout exceptions" in new AsyncScope {
      val f = execution(timeout, NoRetries, {case e: TimeoutException => new CustomExecption(e)}) {
        bar.await()
      }
      Await.result(f) must throwA[CustomExecption]
      bar.release()
    }

    "wrap timeout exception after retry" in new AsyncScope {
      val f = execution(timeout = timeout, retryPolicy = RetryPolicy(retries = 1), onTimeout = {case e: TimeoutException => new CustomExecption(e)}) {
        bar.await()
      }
      Await.result(f) must throwA[CustomExecption]
      bar.release()
    }

    "convert automatically to Scala future" in new AsyncScope {
      import scala.{concurrent => sc}
      import Implicits._

      sc.Await.result(execution {true}, 100 millis) must beTrue
    }
    
  }

  class CustomExecption(cause: Throwable) extends RuntimeException(cause)

  class AsyncScope extends Scope with FuturePerfect {

     val reporter = mock[Reporter[Event]]
     val executorService = Executors.newScheduledThreadPool(4)
     register(reporter)

     class Bar {

       var times = 0

       private val latch = new CountDownLatch(1)

       def succeed() = incAndThen(true)

       def await() = {
         latch.await()
         true
       }

       def release() {
         latch.countDown()
       }

       def explode(e: Throwable): Boolean = incAndThen {
                                                         throw e
                                                       }

       def sleep(millis: Long) = incAndThen {
                                              Thread.sleep(millis)
                                              true
                                            }

       var slept = 1
       def sleepDecreasing(millis: Long) = incAndThen {
        val duration = millis / slept
        slept = slept + 1
        Thread.sleep(duration)
        println("Slept for %s ms".format(duration))
        true
      }

       def explodeThenSucceed() = incAndThen {
         if (times > 1)
           true
         else {
           throw new RuntimeException("Kaboom!")
         }
       }

       def incAndThen[T](f: => T) = {
         times = times + 1
         f
       }

     }

     val bar = new Bar
   }

   val timeout = 100 millis
}
