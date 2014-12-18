package com.wix.async

import scala.annotation.implicitNotFound
import scala.concurrent.duration._
import java.util.concurrent.{TimeoutException, TimeUnit}
import com.twitter.{util => tw}
import scala.language.implicitConversions
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author shaiyallin
 * @since 12/18/13
 */

object Implicits {
  implicit def twitterDuration2ScalaDuration(td: tw.Duration): Duration =
    Duration(td.inNanoseconds, TimeUnit.NANOSECONDS)

  implicit def scalaDuration2TwitterDuration(d: Duration): tw.Duration =
    tw.Duration(d.toNanos, TimeUnit.NANOSECONDS)

  implicit def twitterFuture2ScalaFuture[T](tf: tw.Future[T]): Future[T] = {
    val p = Promise[T]()
    tf respond {
      case tw.Return(r) => p success r
      case tw.Throw(e) => p failure e
    }
    p.future
  }

  implicit def scalaFuture2TwitterFuture[T](f: Future[T]): tw.Future[T] = {
    val p = tw.Promise[T]()

    f.onSuccess { case r: T =>
      p.setValue(r)
    }

    f.onFailure { case e: T =>
      p.setException(e)
    }

    p
  }

  implicit class TimeoutableFuture[T](f: Future[T]) {

    def within(timeout: Duration)(implicit executor: ExecutionContext, scheduler: TerminatorScheduler) = {
      if (timeout == Duration.Zero || f.isCompleted)
        f
      else {
        val p = Promise[T]()

        val terminator = scheduler.schedule(timeout) {
          p.tryFailure(new TimeoutException(s"Future.within timed out after ${timeout.toMillis} millis"))
        }

        f.onComplete { result =>
          p.tryComplete(result)
          terminator.cancel
        }

        p.future
      }
    }
  }
}