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

package monix.reactive.internal.builders

import monix.execution.Cancelable
import monix.reactive.Observable
import monix.reactive.internal.reactivestreams.SingleAssignmentSubscription
import monix.reactive.observers.Subscriber
import org.reactivestreams
import org.reactivestreams.{Publisher => RPublisher, Subscription}

private[reactive] final class ReactiveObservable[A](publisher: RPublisher[A])
  extends Observable[A] {

  def unsafeSubscribeFn(subscriber: Subscriber[A]): Cancelable = {
    val sub = subscriber.toReactive
    val subscription = SingleAssignmentSubscription()

    publisher.subscribe(new reactivestreams.Subscriber[A] {
      def onNext(t: A): Unit = sub.onNext(t)
      def onComplete(): Unit = sub.onComplete()
      def onError(t: Throwable): Unit = sub.onError(t)

      def onSubscribe(s: Subscription): Unit = {
        subscription := s
        sub.onSubscribe(s)
      }
    })

    subscription
  }
}