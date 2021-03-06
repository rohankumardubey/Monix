/*
 * Copyright (c) 2014-2016 by its authors. Some rights reserved.
 * See the project homepage at: https://monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monifu.concurrent.schedulers

import java.util.concurrent.TimeUnit
import monifu.concurrent.Scheduler.{Platform, Environment}
import monifu.concurrent.schedulers.Timer.{clearTimeout, setTimeout}
import monifu.concurrent.{Cancelable, UncaughtExceptionReporter}
import scala.concurrent.duration._

/**
 * An `AsyncScheduler` schedules tasks to happen in the future with
 * the given `ScheduledExecutorService` and the tasks themselves are
 * executed on the given `ExecutionContext`.
 */
final class AsyncScheduler private (reporter: UncaughtExceptionReporter)
  extends ReferenceScheduler {

  def scheduleOnce(initialDelay: FiniteDuration, r: Runnable): Cancelable = {
    val task = setTimeout(initialDelay.toMillis, r, reporter)
    Cancelable(clearTimeout(task))
  }

  def scheduleOnce(initialDelay: Long, unit: TimeUnit, r: Runnable) = {
    val millis = TimeUnit.MILLISECONDS.convert(initialDelay, unit)
    val task = setTimeout(millis, r, reporter)
    Cancelable(clearTimeout(task))
  }

  def execute(runnable: Runnable): Unit = {
    setTimeout(0, runnable, reporter)
  }

  def reportFailure(t: Throwable): Unit =
    reporter.reportFailure(t)

  val env = Environment(256, Platform.JS)
}

object AsyncScheduler {
  def apply(reporter: UncaughtExceptionReporter): AsyncScheduler =
    new AsyncScheduler(reporter)
}
