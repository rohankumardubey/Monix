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

package monifu.reactive.internals.operators

import monifu.reactive.Observable
import scala.concurrent.duration.Duration.Zero

object TakeLeftSuite extends BaseOperatorSuite {
  def sum(sourceCount: Int): Long = sourceCount.toLong * (sourceCount + 1) / 2
  def count(sourceCount: Int) = sourceCount

  def createObservable(sourceCount: Int) = {
    require(sourceCount > 0, "sourceCount should be strictly positive")
    Some {
      val o = if (sourceCount == 1)
        Observable.range(1, 10).take(1)
      else
        Observable.range(1, sourceCount * 2).take(sourceCount)

      Sample(o, count(sourceCount), sum(sourceCount), Zero, Zero)
    }
  }

  def observableInError(sourceCount: Int, ex: Throwable) = {
    require(sourceCount > 0, "sourceCount should be strictly positive")
    Some {
      val o = if (sourceCount == 1)
        createObservableEndingInError(Observable.range(1, 2), ex)
          .take(2)
      else
        createObservableEndingInError(Observable.range(1, sourceCount), ex)
          .take(sourceCount)

      Sample(o, count(sourceCount-1), sum(sourceCount-1), Zero, Zero)
    }
  }

  def brokenUserCodeObservable(sourceCount: Int, ex: Throwable) =
    None
}
