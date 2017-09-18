package com.wix.async

import java.util.concurrent.TimeoutException

import com.wix.async.FuturePerfect._
import org.slf4j.LoggerFactory
import scala.concurrent.duration.Duration

/**
 * @author shaiyallin
 * @since 12/6/13
 */
trait LoggerReporting extends LoggerReportingMessages { this: Reporting[Event] =>

  private val log = LoggerFactory.getLogger(getClass)

  listenFor {
    case Retrying(timeout, remainingRetries, executionName)      => log.warn(retrying(timeout, remainingRetries, executionName))
    case GaveUp(timeout, error, executionName)                   => log.error(gaveUp(timeout, error, executionName))
    case ExceededTimeout(actual, executionName, _)               => log.error(exceededTimeout(actual, executionName))
    case TimeSpentInQueue(time, executionName)                   => log.info(timeSpentInQueue(time, executionName))
    case Successful(elapsed, executionName, _)                   => log.info(successful(elapsed, executionName))
    case Failed(elapsed, error, executionName)                   => log.error(failed(elapsed, error, executionName), error)
    case TimeoutWhileInQueue(timeInQueue, error, executionName)  => log.error(timeoutWhileInQueue(timeInQueue, error, executionName), error)
  }
}

trait LoggerReportingMessages {
  def retrying(timeout: Duration, remainingRetries: Long, executionName: String) = s"Execution [$executionName] timed out after ${timeout.toMillis} ms, retrying $remainingRetries more times."
  def gaveUp(timeout: Duration, e: TimeoutException, executionName: String) = s"Execution [$executionName] timed out after ${timeout.toMillis} ms, giving up."
  def exceededTimeout(actual: Duration, executionName: String) = s"Execution [$executionName] timed out, actual duration was ${actual.toMillis} ms."
  def timeSpentInQueue(time: Duration, executionName: String) = s"Execution [$executionName] started after spending ${time.toMillis} ms in queue."
  def successful(elapsed: Duration, executionName: String) = s"Execution [$executionName] succeeded after ${elapsed.toMillis} ms"
  def failed(elapsed: Duration, error: Throwable, executionName: String) = s"Execution [$executionName] failed after ${elapsed.toMillis} ms."
  def timeoutWhileInQueue(timeInQueue: Duration, e: TimeoutException, executionName: String) = s"Execution [$executionName] timed out after waiting ${timeInQueue.toMillis} in queue."
}
