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

package monifu.reactive.streams

import monifu.concurrent.Scheduler
import monifu.concurrent.atomic.padded.Atomic
import monifu.reactive.Ack.{Cancel, Continue}
import monifu.reactive.{Subscriber, Ack, Observer}
import org.reactivestreams.{Subscriber => RSubscriber, Subscription}

import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.concurrent.{Future, Promise}

/**
 * Wraps a `org.reactivestreams.Subscriber` instance that respects the
 * [[http://www.reactive-streams.org/ Reactive Streams]] contract
 * into an [[monifu.reactive.Observer Observer]] instance that respect the `Observer`
 * contract.
 */
final class ReactiveSubscriberAsMonifuSubscriber[T] private
    (subscriber: RSubscriber[T])
    (implicit val scheduler: Scheduler)
  extends Subscriber[T] {

  if (subscriber == null) throw null
  import monifu.reactive.streams.ReactiveSubscriberAsMonifuSubscriber.RequestsQueue

  private[this] val requests = new RequestsQueue
  private[this] var leftToPush = 0L
  private[this] var firstEvent = true

  @tailrec
  def onNext(elem: T): Future[Ack] = {
    if (firstEvent) {
      firstEvent = false
      subscriber.onSubscribe(createSubscription())
      onNext(elem) // retry
    }
    else if (leftToPush > 0) {
      leftToPush -= 1
      subscriber.onNext(elem)
      Continue
    }
    else {
      requests.await().flatMap { requested =>
        if (requested <= 0) Cancel else {
          leftToPush += (requested - 1)
          subscriber.onNext(elem)
          Continue
        }
      }
    }
  }

  def onError(ex: Throwable): Unit = {
    if (firstEvent) subscriber.onSubscribe(createSubscription())
    subscriber.onError(ex)
  }

  def onComplete(): Unit = {
    if (firstEvent) subscriber.onSubscribe(createSubscription())
    subscriber.onComplete()
  }

  private def createSubscription() = new Subscription {
    def cancel(): Unit = {
      requests.cancel()
    }

    def request(n: Long): Unit = {
      try requests.request(n) catch {
        case ex: IllegalArgumentException =>
          subscriber.onError(ex)
      }
    }
  }
}

object ReactiveSubscriberAsMonifuSubscriber {
  /**
   * Given an `org.reactivestreams.Subscriber` as defined by
   * the [[http://www.reactive-streams.org/ Reactive Streams]]
   * specification, it builds an [[Observer]] instance compliant
   * with the Monifu Rx implementation.
   */
  def apply[T](subscriber: RSubscriber[T])(implicit s: Scheduler): ReactiveSubscriberAsMonifuSubscriber[T] = {
    new ReactiveSubscriberAsMonifuSubscriber[T](subscriber)
  }

  /**
   * An asynchronous queue implementation for dealing with
   * requests from a Subscriber.
   */
  private final class RequestsQueue {
    private[this] val state = Atomic(ActiveState(Queue.empty, Queue.empty) : State)

    @tailrec
    def await(): Future[Long] = {
      state.get match {
        case CancelledState =>
          Future.successful(0)

        case oldState @ ActiveState(elements, promises) =>
          if (elements.nonEmpty) {
            val (e, newQ) = elements.dequeue
            val newState = ActiveState(newQ, promises)

            if (!state.compareAndSet(oldState, newState))
              await()
            else
              Future.successful(e)
          }
          else {
            val p = Promise[Long]()
            val newState = ActiveState(elements, promises.enqueue(p))

            if (!state.compareAndSet(oldState, newState))
              await()
            else
              p.future
          }
      }
    }

    @tailrec
    def request(n: Long): Unit = {
      require(n > 0, "n must be strictly positive, according to " +
        "the Reactive Streams contract, rule 3.9")

      state.get match {
        case CancelledState =>
          () // do nothing

        case oldState @ ActiveState(elements, promises) if promises.nonEmpty =>
          val (p, q) = promises.dequeue
          val newState = ActiveState(elements, q)

          if (!state.compareAndSet(oldState, newState))
            request(n)
          else
            p.success(n)

        case oldState @ ActiveState(Queue(requested), promises) if requested > 0 =>
          val r = requested + n
          val newState = ActiveState(Queue(r), promises)

          if (!state.compareAndSet(oldState, newState))
            request(n)

        case oldState @ ActiveState(elements, promises) =>
          val newState = ActiveState(elements.enqueue(n), promises)
          if (!state.compareAndSet(oldState, newState))
            request(n)
      }
    }

    @tailrec
    def cancel(): Unit = {
      state.get match {
        case CancelledState =>
          () // do nothing

        case oldState @ ActiveState(_, promises) =>
          if (!state.compareAndSet(oldState, CancelledState))
            cancel()
          else
            promises.foreach(_.success(0))
      }
    }

    sealed trait State

    case class ActiveState(elements: Queue[Long], promises: Queue[Promise[Long]])
      extends State

    case object CancelledState
      extends State
  }
}