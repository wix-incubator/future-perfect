package com.wix.async.helpers

import com.wix.async.DelayStrategy
import org.specs2.mock.Mockito

/**
 * User: nadavl
 * Date: 9/11/14
 * Time: 10:47 AM
 */

object TestableDelayStrategy extends Mockito{
  def apply(): DelayStrategy = {
    val delaymock: DelayStrategy = mock[DelayStrategy]
    delaymock.next returns delaymock
    delaymock
  }
}
