package goa.channel

import scala.concurrent.{Future, Promise}

private[goa] class AbstractHandlerContext(val pipeline: Pipeline,
                                          _handler: Handler) extends HandlerContext {
  var prev: AbstractHandlerContext = _

  var next: AbstractHandlerContext = _

  override def handler: Handler = _handler

  override def channel: Channel = pipeline.channel

  override def sendReceived(msg: Object): HandlerContext = {
    if (next != null) {
      next.handler.received(next, msg)
    }
    this
  }

  override def sendConnected(): HandlerContext = {
    if (next != null) {
      next.handler.connected(next)
    }
    this
  }

  override def write(msg: Object): Future[Int] = {
    write(msg, Promise[Int]())
  }

  override def write(msg: Object, promise: Promise[Int]): Future[Int] = {
    if (prev != null) {
      prev.handler.write(prev, msg, promise)
    }
    promise.future
  }
}

private[goa] class AbstractHandlerContextImpl(pipeline: Pipeline,
                                              handler: Handler) extends AbstractHandlerContext(pipeline, handler) {
}