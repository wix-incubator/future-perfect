[![Build Status](https://travis-ci.org/wix/future-perfect.png)](https://travis-ci.org/wix/future-perfect)

Overview
========
Future Perfect is a library wrapping Futures with non-functional concerns such as retry policy, declarative timeouts and lifecycle event hooks.

Motivation
----------
So why did we write yet another asynchronous execution library? At Wix.com, we have dozens of services using legacy Java
infrastructure that is blocking by nature - for instance JDBC, Apache HTTP Client, etc. Still wanting to have async executions
(such as reading from MySQL, calling an RPC endpoint), we turned to using Scala Futures.

However, it soon became apparent that making previously-synchronous services asynchronous introduces a myriad of problems
such as latency-induced cascading failures, errors in the middle of an async execution chain and so on. These problems are
of a non-functional nature, and typically require retrying (for errors) and specifying timeouts using `Await.result`. While
this is definitely possible, it makes the otherwise-declarative nature of the (pseudo) monadic `Future` imperative, and worse -
forcing us to extract the result, thus breaking for comprehensions or monadic transformations.

Furthermore, it's important for us to be able to closely inspect the asynchronous executions, being able to know exactly
what's happening in our DB access or RPC code. One of the drawbacks of using Futures out-of-the-box is that we generally
lose the ability to meter execution times using off-the-shelf products. In addition, when retrying on errors or timeouts,
we lose the ability to count or monitor said errors or timeouts.

On Scala vs Twitter's Futures
-----------------------------
We chose to use Twitter's Future implementation rather than Scala's native Futures because:
 * Twitter's Future has the `Future.within` method, allowing us to specify a declarative timeout rather than an imperative one.
 * Twitter's Future `.onSuccess`, `.onFailure` and `.rescue` methods return a chained future, allowing us to chain callbacks. In contrast, the counterpart methods in Scala's Future implementation are not chainable, allowing us to only specify a single callback for each type.

Getting Started
===============
Future Perfect `0.1.10` is available for Scala 2.10 and 2.11 on Maven Central Repository. The next milestone is `0.1.11-SNAPSHOT` and is available from the Sonatype snapshots repository.

SBT
---

Simply add the `future-perfect` module to your build settings:

```scala
libraryDependencies += "com.wix" %% "future-perfect" % "0.1.10"
```

If you want to evaluate the upcoming snapshot release, add the Sonatype snapshot repository to your resolvers; typically this means adding the following to your `build.sbt` file:

```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "com.wix" %% "future-perfect" % "0.1.10-SNAPSHOT"
```

Maven
-----

Simply add the appropriate dependency to your POM:

```xml
<dependencies>
  <dependency>
    <groupId>com.wix</groupId>
    <artifactId>future-perfect_${scala.tools.version}</artifactId>
    <version>0.1.10</version>
  </dependency>
</dependencies>
```

To use the snapshot version, add the Sonatype snapshot repository and a dependency on Future Perfect to your POM:

```xml
<repositories>
  <repository>
    <id>sonatype-snapshots</id>
    <name>Sonatype snapshot repository</name>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <releases><enabled>false</enabled></releases>
    <snapshots><enabled>true</enabled></snapshots>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.wix</groupId>
    <artifactId>future-perfect_${scala.tools.version}</artifactId>
    <version>0.1.11-SNAPSHOT</version>
  </dependency>
</dependencies>
```

Usage
=====
To use Future Perfect you'll need to extend the trait `FuturePerfect` and provide an instance of `ScheduledExecutorService`.
We expect a `ScheduledExecutorService` rather than a simple `ExecutorService` due to the way Twitter's Future implements timeouts;
the `Future.within()` method scheduled a task to awaken after the timeout has passed, and if the future's promise hasn't been
completed yet, it completes it with a `TimeoutException` failure.

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

By default, Future Perfect returns an instance of `com.twitter.util.Future`. If you want to use Scala futures instead,
just import `com.wix.Async.Implicits._`, which provides an implicit conversion from Twitter Future to Scala Future.

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

By default, if an execution times out, an exception of type `TimeoutGaveUpException` is thrown. This can be changed by
providing the `onTimeout` argument with a partial function from `TimeoutException` to `Exception`:

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
Future Perfect will not retry. Specifying a retry policy with one retry without specifying `shouldRetry` will run the execution
at most 2 times, retrying on all exceptions. You can use a preset `RetryStrategy` such as `onCheckedException`, `onlyOnTimeout`
or `on[FooException]`, or you may provide your own `(Throwable => Boolean)`.

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

* `Successful` - the specified execution has completed successfully within the specified duration.
* `Failed` - the specified execution has failed with the specified exception within the specified duration.
* `Retrying` - the specified execution has failed and will be retrying *n* more times.
* `GaveUp` - the specified execution has failed with the specified exception within the specified duration and will not be retrying.
* `ExceededTimeout` - the specified execution has timed out; the *actual* variable represents the total time it took the blocking call to complete.
* `TimeSpentInQueue` - the specified execution has waited for the specified duration in the `ExecutorService`'s queue before starting.
* `TimeoutWhileInQueue` - the specified execution has timed out after waiting for the specified duration in the `ExecutorService`, before even starting.

Adding a listener is easy, just add the appropriate trait to the class extending `FuturePerfect`:
```scala
import com.wix.async._

object App extends FuturePerfect with LoggerReporting {
    val executorService = Executors.newScheduledThreadPool(10)

    val future = execution {
        // some blocking call
    }

    val result = Await.result(future)
}
```

Roadmap
=======
* Metric-reporting Executor Service
