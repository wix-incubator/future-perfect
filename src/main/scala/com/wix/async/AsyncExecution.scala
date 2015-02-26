package com.wix.async

import java.util.concurrent.ExecutorService

import com.twitter.util._
import com.wix.async.FuturePerfect.{Event, _}
import com.wix.async.Implicits._

import scala.concurrent.duration.Duration

/**
 * User: nadavl
 * Date: 9/13/14
 * Time: 5:57 PM
 */
private object AsyncExecution {
  protected[async] lazy val timer: Timer = new ScheduledThreadPoolTimer()
}

protected[async] class AsyncExecution[T](executorService: ExecutorService,
                                timeout: Duration,
                                retryPolicy: RetryPolicy,
                                onTimeout: TimeoutHandler,
                                executionName: String,
                                report: (Event => Unit) ) {

  def apply(blockingExecution: => T): Future[T] = execute(retryPolicy)(blockingExecution)

  protected lazy val pool = FuturePool(executorService)
  @volatile private var started = false
  private def execute(retryPolicy: RetryPolicy)(blockingExecution: => T): Future[T] = {
    val submittedToQueue = Stopwatch.start()

    var future = submitToAsyncExecution {
      decorateWithReporting(submittedToQueue) {
        blockingExecution
      }
    }

    if (timeout != Duration.Zero)
      future = future.within(AsyncExecution.timer, timeout)

    future.rescue {
      case e: Throwable if retryPolicy.shouldRetryFor(e) =>
        report(Retrying(timeout, retryPolicy.retries, executionName))
        retryPolicy.delayStrategy.delay()
        execute(retryPolicy.next)(blockingExecution)

      case e: TimeoutException =>
        if (started)
          report(GaveUp(timeout, e, executionName))
        else
          report(TimeoutWhileInQueue(submittedToQueue(), e, executionName))

        throw onTimeout.applyOrElse(e, (cause: TimeoutException) => TimeoutGaveUpException(cause, executionName, timeout))

    }.onSuccess { t: T =>
      report(Successful(submittedToQueue(), executionName, t))
    }.onFailure { error =>
      report(Failed(submittedToQueue(), error, executionName))
    }
  }

  private def submitToAsyncExecution(f: => T) = pool(f)

  /**
   * Wraps the code to be executed with a reporting block
   * @return
   */
  def decorateWithReporting(submittedToQueue: Stopwatch.Elapsed)(nested: => T) = {
    started = true
    report(TimeSpentInQueue(submittedToQueue(), executionName))

    val elapsedInBlockingCall = Stopwatch.start()
    val res: T = nested
    val duration = elapsedInBlockingCall()
    if (timeout != Duration.Zero && duration > timeout) {
      report(ExceededTimeout(duration, executionName, res))
    }
    res
  }
}
