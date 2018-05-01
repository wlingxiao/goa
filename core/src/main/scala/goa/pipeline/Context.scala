package goa.pipeline

import scala.concurrent.{Future, Promise}

class Context(
               var prev: Context,
               var next: Context,
               val handler: Handler,
               val pipeline: Pipeline) {

  def sendRead(msg: Object): Unit = {
    if (next != null) {
      next.handler.received(next, msg)
    }
  }

  def write(msg: Object): Future[Int] = {
    val promise = Promise[Int]()
    if (prev != null) {
      prev.handler.write(prev, msg, promise)
    }
    promise.future
  }

  def write(msg: Object, promise: Promise[Int]): Future[Int] = {
    if (prev != null) {
      prev.handler.write(prev, msg, promise)
    }
    promise.future
  }
}