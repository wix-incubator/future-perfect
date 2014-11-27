package com.wix.async

import java.util.concurrent.Executors

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
      Await.result(future)
    }
    val timeout = 100 millis
  }

  "within" should {
    "comeback on time" in new Context {
      val f = Future { 1 }
      val timeoutableF = TimeoutableFuture.within(f, new Timer, timeout)

      theExecutionOf(timeoutableF) must throwA[TimeoutGaveUpException]
    }
  }
}

