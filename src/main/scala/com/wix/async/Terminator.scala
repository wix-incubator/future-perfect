package com.wix.async

import java.util.concurrent.{TimeUnit, ScheduledThreadPoolExecutor}

import scala.annotation.implicitNotFound
import scala.concurrent.duration.Duration


trait Terminator {
  def cancel: Unit
}

@implicitNotFound("No implicit TerminatorScheduler found. Define your own or import com.wix.async.TerminatorScheduler.Global")
trait TerminatorScheduler {
  def schedule(timeout: Duration)(f: => Unit): Terminator
}

object TerminatorScheduler {
  implicit lazy val Global = new ScheduledThreadPoolTerminator
}

class ScheduledThreadPoolTerminator extends TerminatorScheduler {
  val scheduler = new ScheduledThreadPoolExecutor(1)
  scheduler.setMaximumPoolSize(5)

  def schedule(timeout: Duration)(f: => Unit): Terminator = {
    val runnable = new Runnable { def run() = f }
    val javaf = scheduler.schedule(runnable, timeout.toMillis, TimeUnit.MILLISECONDS)

    new Terminator {
      def cancel: Unit = {
        javaf.cancel(true)
        scheduler.remove(runnable)
      }
    }
  }
}