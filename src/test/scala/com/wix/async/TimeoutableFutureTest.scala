package com.wix.async

import java.util.concurrent.{TimeUnit, ScheduledThreadPoolExecutor, CountDownLatch, Executors}

import com.twitter.util.TimerTask
import com.wix.async.FuturePerfect.Successful
import com.wix.async.helpers.FooOperation
import org.specs2.mutable.{SpecificationWithJUnit, Specification}
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import java.util.Timer

/**
 * Created by Igor_Glizer on 11/27/14.
 */
class TimeoutableFutureTest extends SpecificationWithJUnit with NoTimeConversions {

  trait Context extends Scope with FuturePerfect {
    val bar = new FooOperation
    val executorService = Executors.newFixedThreadPool(4)

    def theExecutionOf[T] = waitFor[T] _

    def waitFor[T](future: Future[T]): T = {
      Await.result(future, 1.second)
    }
    val timeout = 100.millis

    implicit class TimeoutableFuture[T](f: Future[T]) {

      def within(timeout: Duration) = {
        val p = Promise[T]()

        val terminator = Terminator(timeout) {
          p.tryFailure(new TimeoutException(s"Future.within timed out after ${timeout.toMillis} millis"))
        }

        f.onComplete { result =>
          p.tryComplete(result)
          terminator.cancel
        }

        p.future
      }

      class Terminator(timeout: Duration, f: => Unit) {
        import Terminator._
        val runnable = new Runnable { def run() = f }
        val javaf = scheduler.schedule(runnable, timeout.toMillis, TimeUnit.MILLISECONDS)

        def cancel = {
          javaf.cancel(true)
          scheduler.remove(runnable)
        }
      }

      object Terminator {
        val scheduler = new ScheduledThreadPoolExecutor(1)
        scheduler.setMaximumPoolSize(5)

        def apply(timeout: Duration)(f: => Unit) = new Terminator(timeout, f)
      }
    }
  }

  "within" should {
    "comeback on time" in new Context {
      val f = Future { 1 }
      theExecutionOf(f.within(timeout)) must beEqualTo(1)
    }

    "fail if timeout has passed" in new Context {
      val latch = new CountDownLatch(1)
      val f = Future {
        latch.await()
      }

      theExecutionOf(f.within(timeout)) must throwA[TimeoutException]("Future.within timed out after 100 millis")

      latch.countDown()
    }
  }
}

