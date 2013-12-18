package com.wix.async

import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import com.twitter.{util => tw}
import scala.language.implicitConversions

/**
 * @author shaiyallin
 * @since 12/18/13
 */

object Implicits {
  implicit def twitterDuration2ScalaDuration(td: tw.Duration): Duration =
    Duration(td.inNanoseconds, TimeUnit.NANOSECONDS)

  implicit def scalaDuration2TwitterDuration(d: Duration): tw.Duration =
    tw.Duration(d.toNanos, TimeUnit.NANOSECONDS)
}