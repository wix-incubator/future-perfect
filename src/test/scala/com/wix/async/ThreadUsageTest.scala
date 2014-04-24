package com.wix.async

import com.twitter.util.Await
import java.util.concurrent.{Executors, ExecutorService}
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import scala.concurrent.duration._

/**
 * User: nadavl
 * Date: 4/24/14
 * Time: 11:18 AM
 */
class ThreadUsageTest extends SpecificationWithJUnit with NoTimeConversions{
  "creating a FuturePerfect class" should {
    "not increase the thread count when new FP classes are created" in new Context {
      val baseThreadCount = Thread.activeCount()
      for(i <- 1 to 100){
        val myAsyncClass = newFuturePerfectClass
        Await.result(myAsyncClass.runSomethingAsync)
      }
      val newThreadCount = Thread.activeCount()
      newThreadCount must_== baseThreadCount + executionThreadPoolSize + twitterTimerThreadCount
    }
  }

  trait Context extends Scope{
    val twitterTimerThreadCount = 2
    val executionThreadPoolSize = 4
    val executorService = Executors.newFixedThreadPool(executionThreadPoolSize)
    def newFuturePerfectClass = new FuturePerfectClass(executorService)
  }
  
  class FuturePerfectClass(val executorService: ExecutorService) extends FuturePerfect {
    def runSomethingAsync = execution(timeout = 10 millis) {
      "run lola run"
    }

  }
}
