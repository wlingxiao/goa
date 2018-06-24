package goa

import goa.http1.{HttpRequest, HttpResponsePrelude}
import goa.logging.Logging
import goa.pipeline.{Handler, Context => HanclerContext}
import goa.util.BufferUtils

private class Dispatcher(app: Goa) extends Handler with Logging {
  override def received(ctx: HanclerContext, msg: Object): Unit = {
    val httpRequest = msg.asInstanceOf[HttpRequest]
    val request = Request(app.bodyReader, httpRequest)
    val response = Response()
    try {
      Goa.putMessage((request, response))
      app.middlewareChain.messageReceived()
    } finally {
      Goa.clearMessage()
    }

    val prelude = HttpResponsePrelude(response.status.code, response.status.reason, response.headers.toSeq)
    val body = if (response.body != null) {
      response.body
    } else BufferUtils.emptyBuffer
    ctx.write(prelude -> body)
  }
}
