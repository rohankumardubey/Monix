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
import monifu.reactive.exceptions.DummyException
import scala.concurrent.duration.Duration.Zero

object OnErrorFallbackToSuite extends BaseOperatorSuite {
  def createObservable(sourceCount: Int) = Some {
    val obs = Observable.range(0, sourceCount)
      .endWithError(DummyException("expected"))
      .onErrorFallbackTo(Observable.range(0, 10))

    val sum = sourceCount * (sourceCount-1) / 2 + 9 * 5
    Sample(obs, sourceCount+10, sum, Zero, Zero)
  }

  def observableInError(sourceCount: Int, ex: Throwable) = None

  def brokenUserCodeObservable(sourceCount: Int, ex: Throwable) = Some {
    val obs = Observable.range(0, sourceCount)
      .endWithError(DummyException("expected"))
      .onErrorFallbackTo { throw ex }

    val sum = 1L * sourceCount * (sourceCount-1) / 2
    Sample(obs, sourceCount, sum, Zero, Zero)
  }
}
