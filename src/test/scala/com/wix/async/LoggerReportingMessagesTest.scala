package com.wix.async

import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import scala.concurrent.duration._

class LoggerReportingMessagesTest extends SpecificationWithJUnit with NoTimeConversions {

  trait Context extends Scope with LoggerReportingMessages

  "logger reporting messages" should {
    "correctly format params in ExceededTimeout message with params" in new Context {
      val duration = 1 second
      val name = "exec name"
      val params = Map("Value1" -> "10.0", "Value2" -> "example.com")
      val message = exceededTimeout(duration, name, params)
      message must_== s"Execution [$name] timed out, actual duration was ${duration.toMillis} ms. Additional params: Value1=[10.0], Value2=[example.com]"
    }

    "correctly format params in ExceededTimeout message without params" in new Context {
      val duration = 1 second
      val name = "exec name"
      val message = exceededTimeout(duration, name, Map())
      message must_== s"Execution [$name] timed out, actual duration was ${duration.toMillis} ms."
    }
  }
}
