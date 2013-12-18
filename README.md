[![Build Status](https://travis-ci.org/wix/future-perfect.png)](https://travis-ci.org/wix/future-perfect)

Overview
========

Future Perfect is a library wrapping Futures with non-functional concerns such as retry policy, declarative timeouts and event reporting hooks.

We chose to use Twitter's Future implementation rather than Scala's native Futures because:
 * Twitter's Future has the *Future.within* method, allowing us to specify a declarative timeout rather than an imperative one.
 * Twitter's Future *.onSuccess*, *.onFailure* and *.rescue* methods return a chained future, allowing us to chain callbacks. In contrast, the counterpart methods in Scala's Future implementation are not chainable, allowing us to only specify a single callback for each type.

Getting Started
===============

Usage
=====
To use Future Perfect you need to extends the trait FuturePerfect and provide an instance of ScheduledExecutorService.
We expect a *ScheduledExecutorService* rather than a simple *ExecutorService* due to the way Twitter's Future implements timeouts;
the *Future.within()* method scheduled a task to awaken after the timeout has passed, and if the future's promise hasn't been
completed yet, it completes it with a *TimeoutException* failure.

Keep in mind that if using timeouts, you'll need a thread pool of size 10 in order to run 5 executions in parallel, since
for each execution we'll create a timeout task.

For comprehensive usage examples, please refer to the unit tests.

Basic execution
---------------
```scala
import com.wix.async._

object App extends FuturePerfect {
    val executorService = Executors.newScheduledThreadPool(10)

    val future = execution {
        // some blocking call
    }

    val result = Await.result(future)
}
```

You can give your execution a name; this will be used in exception messages, as well as reported with lifecycle events (see below):

```scala
import com.wix.async._

object App extends FuturePerfect {
    val executorService = Executors.newScheduledThreadPool(10)

    val future = execution(name = "foo") {
        // some blocking call
    }

    val result = Await.result(future)
}
```

Timeouts
--------
You can specify an execution timeout by passing an instance of Duration to the execution() method.

```scala
import com.wix.async._
import scala.concurrent.duration._

object App extends FuturePerfect {
    val executorService = Executors.newScheduledThreadPool(10)

    val future = execution(timeout = 100 millis) {
        // some blocking call
    }

    val result = Await.result(future)
}
```

By default, if an execution times out, an exception of type *TimeoutGaveUpException* is thrown. This can be changed by
providing the *onTimeout* argument with a partial function from *TimeoutException* to *Exception*:

```scala
import com.wix.async._
import scala.concurrent.duration._
import com.twitter.util.TimeoutException

object App extends FuturePerfect {
    val executorService = Executors.newScheduledThreadPool(10)

    val future = execution(
        timeout = 100 millis,
        onTimeout = {case e: TimeoutException => new CustomExecption(e)}) {

        // some blocking call
    }

    val result = Await.result(future)
}
```

Retrying
--------

You can specify how many retries an execution should have if it fails, as well as which conditions to retry on. By default,
Future Perfect will not retry. Specifying a retry policy with one retry without specifying *shouldRetry* will run the execution
at most 2 times, retrying on all exceptions. You can use a preset *RetryStrategy* such as *onCheckedException*, *onlyOnTimeout*
or *on[FooException]*, or you may provide your own *(Throwable => Boolean)*.

```scala
import com.wix.async._
import scala.concurrent.duration._

object App extends FuturePerfect {
    val executorService = Executors.newScheduledThreadPool(10)

    val future = execution(
        timeout = 100 millis,
        retryPolicy = RetryPolicy(retries = 1, shouldRetry = onlyOnTimeout)) {

        // some blocking call
    }

    val result = Await.result(future)
}
```

Execution Lifecycle
-------------------
Throughout the life cycle of an execution, different events are being reported by FuturePerfect, including execution time,
execution name and other info such as the exception (for failure events). You may add listeners for these events, handling
some or all of them. Refer to
[LoggerReporting.scala](https://github.com/wix/future-perfect/blob/master/src/main/scala/com/wix/async/LoggerReporting.scala)
for an example on how to write such a listener.

* _Successful_ - the specified execution has completed successfully within the specified duration.
* _Failed_ - the specified execution has failed with the specified exception within the specified duration.
* _Retrying_ - the specified execution has failed and will be retrying *n* more times.
* _GaveUp_ - the specified execution has failed with the specified exception within the specified duration and will not be retrying.
* _ExceededTimeout_ - the specified execution has timed out; the *actual* variable represents the total time it took the blocking call to complete.
* _TimeSpentInQueue_ - the specified execution has waited for the specified duration in the *ExecutorService*'s queue before starting.
* _TimeoutWhileInQueue_ - the specified execution has timed out after waiting for the specified duration in the *ExecutorService*, before even starting.

Roadmap
=======
* Implicit conversion support for Scala futures
