package com.wix.async

import java.util.concurrent.{CountDownLatch, Executors}

import com.wix.async.helpers.FooOperation
import com.wix.async.Implicits._
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Success
import com.wix.async.TerminatorScheduler.Global

class TimeoutableFutureTest extends SpecificationWithJUnit with NoTimeConversions {

  trait Context extends Scope with FuturePerfect {
    val bar = new FooOperation
    val executorService = Executors.newFixedThreadPool(4)

    def theExecutionOf[T] = waitFor[T] _

    def waitFor[T](future: Future[T]): T = {
      Await.result(future, 1.second)
    }
    val timeout = 100.millis

    def aFuture = Future { 1 }

  }

  "within" should {
    "succeed if the future completed within specified timeout" in new Context {
      val f = aFuture
      theExecutionOf(f.within(timeout)) must beEqualTo(1)
    }

    "return the same future when timeout is forever" in new Context {
      val p = Promise[Int]()
      val f = p.future
      f.within(Duration.Zero) must beTheSameAs(f)
    }

    "return the same future if it's already satisfied" in new Context {
      val p = Promise[Int]()
      val f = p.future
      p.complete(Success(1))

      f.within(timeout) must beTheSameAs(f)
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

