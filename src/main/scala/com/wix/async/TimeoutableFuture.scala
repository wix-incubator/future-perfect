package com.wix.async

import java.util.concurrent.atomic.AtomicInteger
import java.util.Timer
import scala.collection.generic.CanBuildFrom
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Success, Try}

/**
 * Created by Igor_Glizer on 11/27/14.
 */
case class TimeoutableFuture[T](future : Future[T]) extends Awaitable[T]{

  @scala.throws[InterruptedException](classOf[InterruptedException])
  @scala.throws[TimeoutException](classOf[TimeoutException])
  override def ready(atMost: Duration)(implicit permit: CanAwait): TimeoutableFuture[T] = new TimeoutableFuture[T](future.ready(atMost))

  @scala.throws[Exception](classOf[Exception])
  override def result(atMost: Duration)(implicit permit: CanAwait): T = future.result(atMost)
}

object TimeoutableFuture {
  def within[T] (f : Future[T], timer: Timer, timeout: Duration): TimeoutableFuture[T] = new TimeoutableFuture(f)
}
