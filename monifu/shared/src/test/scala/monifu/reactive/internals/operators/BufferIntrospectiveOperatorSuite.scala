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
import scala.concurrent.duration.Duration

object BufferIntrospectiveOperatorSuite extends BaseOperatorSuite {
  def createObservable(sourceCount: Int) = Some {
    val o = Observable.range(0, sourceCount)
      .bufferIntrospective(10)
      .flatMap(list => Observable.fromIterable(list))

    Sample(o, sourceCount, sourceCount * (sourceCount-1) / 2, Duration.Zero, Duration.Zero)
  }

  def observableInError(sourceCount: Int, ex: Throwable) = Some {
    val o = Observable.range(0, sourceCount).endWithError(ex)
      .bufferIntrospective(10)
      .flatMap(list => Observable.fromIterable(list))

    val count = (sourceCount / 10) * 10
    Sample(o, count, count * (count-1) / 2, Duration.Zero, Duration.Zero)
  }

  def brokenUserCodeObservable(sourceCount: Int, ex: Throwable) = None
}
