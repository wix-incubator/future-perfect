package com.wix.async

import scala.concurrent.duration.Duration

import java.util.concurrent.ExecutorService
import FuturePerfect._
import Implicits._
import com.twitter.util.{Future, FuturePool, ScheduledThreadPoolTimer, Timer, Stopwatch, TimeoutException}

/**
 * @author shaiyallin
 * @since 10/31/12
 */

trait FuturePerfect extends Reporting[Event] {

  def executorService: ExecutorService

  private lazy val timer: Timer = new ScheduledThreadPoolTimer()

  class AsyncExecution[T](executorService: ExecutorService,
                          timeout: Duration,
                          retryPolicy: RetryPolicy,
                          onTimeout: TimeoutHandler,
                          executionName: String) {

    private[this] def submitToAsyncExecution(f: => T) = pool(f)
    protected[this] lazy val pool = FuturePool(executorService)

    private var started = false

    def apply(blockingExecution: => T): Future[T] = execute(retryPolicy)(blockingExecution)

    private[this] def execute(retryPolicy: RetryPolicy)(blockingExecution: => T): Future[T] = {
      val submittedToQueue = Stopwatch.start()

      /**
       * Wraps the code to be executed with a reporting block
       * @return
       */
      def decorateWithReporting(nested: => T) = {

        started = true
        report(TimeSpentInQueue(submittedToQueue(), executionName))

        val elapsedInBlockingCall = Stopwatch.start()
        val res: T = nested
        val duration = elapsedInBlockingCall()
        if (duration > timeout) {
          report(ExceededTimeout(duration, executionName, res))
        }

        res
      }

      var future = submitToAsyncExecution {
        decorateWithReporting {
          blockingExecution
        }
      }

      if (timeout != Duration.Zero)
        future = future.within(timer, timeout)

      future.rescue {
        case e: Throwable if retryPolicy.shouldRetryFor(e) =>
          report(Retrying(timeout, retryPolicy.retries, executionName))
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
  }

  // for some reason default parameters don't work for a curried function so I had to supply all permutations
  def execution[T](retryPolicy: RetryPolicy)(f: => T): Future[T] = execution(Duration.Zero, retryPolicy)(f)
  def execution[T](timeout: Duration)(f: => T): Future[T] = execution(timeout, NoRetries)(f)
  def execution[T](timeout: Duration, retryPolicy: RetryPolicy)(f: => T): Future[T] = execution(timeout, retryPolicy,  PartialFunction.empty)(f)
  def execution[T](f: => T): Future[T] = execution(Duration.Zero, NoRetries)(f)
  def execution[T](timeout: Duration = Duration.Zero,
                   retryPolicy: RetryPolicy = NoRetries,
                   onTimeout: TimeoutHandler = PartialFunction.empty,
                   name: String = defaultName)(blockingExecution: => T): Future[T] =

    new AsyncExecution[T](executorService, timeout, retryPolicy, onTimeout, name).apply(blockingExecution)

  private def defaultName = "async"
}

object FuturePerfect {
  type TimeoutHandler = PartialFunction[TimeoutException, Exception]

  sealed trait Event {
    def executionName: String
  }
  case class TimeoutWhileInQueue(timeInQueue: Duration, e: TimeoutException, executionName: String) extends Event
  case class TimeSpentInQueue(time: Duration, executionName: String) extends Event
  case class Retrying(timeout: Duration, remainingRetries: Long, executionName: String) extends Event
  case class GaveUp(timeout: Duration, e: TimeoutException, executionName: String) extends Event
  case class ExceededTimeout(actual: Duration, executionName: String, result: Any) extends Event
  case class Successful(elapsed: Duration, executionName: String, result: Any) extends Event
  case class Failed(elapsed: Duration, error: Throwable, executionName: String) extends Event
}

case class TimeoutGaveUpException(cause: TimeoutException, name: String, timeout: Duration)
  extends RuntimeException(s"Execution $name timed out after ${timeout.toMillis} ms, giving up.", cause)




