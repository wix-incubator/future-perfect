package com.wix.async

import org.specs2.mutable.SpecificationWithJUnit


/**
 * @author shaiyallin
 * @since 12/5/13
 */

class ReportingTest extends SpecificationWithJUnit {

  "a class with Reporting support" should {
    "notify all registered reporters" in {
      var messages: Seq[String] = Seq.empty

      case class Event(message: String)

      trait Reporter1 { this: Reporting[Event] =>
        listenFor { case e =>
          messages +:= s"reporter 1 got event ${e.message}"
        }

      }

      trait Reporter2 { this: Reporting[Event] =>
        listenFor { case e =>
          messages +:= s"reporter 2 got event ${e.message}"
        }

      }

      class Bar extends Reporting[Event] {
        def foo(message: String) {
          report(Event(message))
        }
      }

      val b = new Bar() with Reporter1 with Reporter2
      b.foo("baz")

      messages must contain("baz")
      messages must contain("reporter 1")
      messages must contain("reporter 2")
    }

    "handle a partial function that doesn't match" in {
      var events: Seq[Event] = Seq.empty

      trait Event
      case class Event1(message: String) extends Event
      case class Event2(message: String) extends Event

      trait Reporter { this: Reporting[Event] =>
        listenFor {
          case e: Event1 => events +:= e
        }
      }

      class Bar extends Reporting[Event] with Reporter {
        def foo(message: String) {
          report(Event1(message))
          report(Event2(message))
        }
      }

      val b = new Bar() with Reporter
      b.foo("baz")

      events must contain(Event1("baz"))
      events must not contain(Event2("baz"))
    }
  }
}