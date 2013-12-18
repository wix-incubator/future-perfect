package com.wix.async

/**
 * @author shaiyallin
 * @since 12/5/13
 */

trait Reporter[E] {
  def report(event: E)
}

trait Reporting[E] {
  type R = Reporter[E]
  private[this] var reporters: Seq[R] = Nil

  protected[this] def report(event: E) {
    reporters.foreach(_.report(event))
  }

  protected[this] def listenFor(notification: PartialFunction[E, Unit]) {
    register(new R {
      def report(e: E) {
        if (notification.isDefinedAt(e)) notification(e)
      }
    })
  }

  protected[this] def register(reporter: R) {
    reporters :+= reporter
  }
}