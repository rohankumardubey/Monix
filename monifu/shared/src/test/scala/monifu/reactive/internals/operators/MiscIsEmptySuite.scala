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
import concurrent.duration.Duration.Zero

object MiscIsEmptySuite extends BaseOperatorSuite {
  def createObservable(sourceCount: Int) = Some {
    val shouldBeEmpty = (sourceCount % 2) == 0
    val sum = if (shouldBeEmpty) 2L else 1L

    val source = if (shouldBeEmpty)
      Observable.empty
    else
      Observable.range(0, sourceCount)

    val o = source.isEmpty.map(x => if (x) 2L else 1L)
    Sample(o, 1, sum, Zero, Zero)
  }

  def observableInError(sourceCount: Int, ex: Throwable) = Some {
    val o = Observable.error(ex)
      .isEmpty.map(x => if (x) 1L else 0L)

    Sample(o, 0, 0, Zero, Zero)
  }

  def brokenUserCodeObservable(sourceCount: Int, ex: Throwable) =
    None
}
