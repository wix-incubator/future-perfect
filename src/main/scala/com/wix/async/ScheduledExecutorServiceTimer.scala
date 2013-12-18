package com.wix.async

import com.twitter.util._
import java.util.concurrent._

/**
 * @author shaiyallin
 * @since 1/13/13
 */

class ScheduledExecutorServiceTimer(underlying: ScheduledExecutorService) extends Timer {

  def schedule(when: Time)(f: => Unit): TimerTask = {
    val runnable = new Runnable { def run = f }
    val javaFuture = underlying.schedule(runnable, when.sinceNow.inMillis, TimeUnit.MILLISECONDS)
    new TimerTask { def cancel() { javaFuture.cancel(true) } }
  }

  def schedule(when: Time, period: Duration)(f: => Unit): TimerTask =
    schedule(when.sinceNow, period)(f)

  def schedule(wait: Duration, period: Duration)(f: => Unit): TimerTask = {
    val runnable = new Runnable { def run = f }
    val javaFuture = underlying.scheduleAtFixedRate(runnable,
      wait.inMillis, period.inMillis, TimeUnit.MILLISECONDS)
    new TimerTask { def cancel() { javaFuture.cancel(true) } }
  }

  def stop() = underlying.shutdown()
}