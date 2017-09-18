package com.wix.async

import java.util.concurrent.{TimeoutException, ExecutorService}

import com.wix.async.FuturePerfect._

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
 * @author shaiyallin
 * @since 10/31/12
 */
trait FuturePerfect extends Reporting[Event] {
  def executorService: ExecutorService

  // for some reason default parameters don't work for a curried function so I had to supply all permutations
  def execution[T](retryPolicy: RetryPolicy)(f: => T): Future[T] = execution(Duration.Zero, retryPolicy)(f)
  def execution[T](timeout: Duration)(f: => T): Future[T] = execution(timeout, NoRetries)(f)
  def execution[T](timeout: Duration, retryPolicy: RetryPolicy)(f: => T): Future[T] = execution(timeout, retryPolicy,  PartialFunction.empty)(f)
  def execution[T](f: => T): Future[T] = execution(Duration.Zero, NoRetries)(f)
  def execution[T](timeout: Duration = Duration.Zero,
                   retryPolicy: RetryPolicy = NoRetries,
                   onTimeout: TimeoutHandler = PartialFunction.empty,
                   name: String = defaultName)(blockingExecution: => T): Future[T] =
    new AsyncExecution[T](executorService, timeout, retryPolicy, onTimeout, name, report).apply(blockingExecution)

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




