package bleak
package netty

import scala.concurrent.Future

private[netty] class DefaultPipeline(sessionManager: SessionManager) {

  import DefaultPipeline._

  private val head = new DefaultContext(null, null, new DefaultMiddleware(), "HEAD", this, null, null)

  private val tail = new DefaultContext(head, null, new DefaultMiddleware(), "TAIL", this, null, null)

  head.nextCtx = tail
  tail.prevCtx = head

  def append(middlewares: Middleware*): this.type = {
    middlewares.foreach { middleware =>
      val name = middleware.getClass.getSimpleName
      addLast(name, middleware)
    }
    this
  }

  def append(name: String)(middleware: Middleware): this.type = {
    addLast(name, middleware)
  }

  def prepend(middlewares: Middleware*): this.type = {
    middlewares.foreach(addFirst(null, _))
    this
  }

  def prepend(name: String)(middleware: Middleware): this.type = {
    addFirst(name, middleware)
  }

  def addAfter(baseName: String, name: String, middleware: Middleware): this.type = {
    val ctx = findContext(baseName)
    val newCtx = newContext(name, middleware)
    newCtx.prevCtx = ctx
    newCtx.nextCtx = ctx.nextCtx
    ctx.nextCtx.prevCtx = newCtx
    ctx.nextCtx = newCtx
    this
  }

  private def findContext(name: String): DefaultContext = {
    var ctx = head.nextCtx
    while (ctx != tail) {
      if (ctx.name.equals(name)) {
        return ctx
      }
      ctx = ctx.nextCtx
    }
    null
  }

  def addBefore(baseName: String, name: String, middleware: Middleware): this.type = {
    val ctx = findContext(baseName)
    val newCtx = newContext(name, middleware)
    newCtx.prevCtx = ctx.prevCtx
    newCtx.nextCtx = ctx
    ctx.prevCtx.nextCtx = newCtx
    ctx.prevCtx = newCtx
    this
  }

  def addFirst(name: String, middleware: Middleware): this.type = {
    val newCtx = newContext(name, middleware)
    val nextCtx = head.nextCtx
    newCtx.prevCtx = head
    newCtx.nextCtx = nextCtx
    head.nextCtx = newCtx
    nextCtx.prevCtx = newCtx
    this
  }

  def addLast(name: String, middleware: Middleware): this.type = {
    val prev = tail.prevCtx
    val newCtx = newContext(name, middleware)
    newCtx.prevCtx = prev
    newCtx.nextCtx = tail
    prev.nextCtx = newCtx
    tail.prevCtx = newCtx
    this
  }

  private def newContext(name: String, middleware: Middleware): DefaultContext = {
    new DefaultContext(null, null, middleware, name, this, null, null)
  }

  def insert(baseName: String)(name: String, middleware: Middleware): this.type = {
    addAfter(baseName, name, middleware)
  }

  def received(request: Request, response: Response): Future[Context] = {
    head.message(request, response).received()
  }

  def session(ctx: DefaultContext): Option[Session] = {
    sessionManager.session(ctx)
  }

}

private object DefaultPipeline {

  def apply(sessionManager: SessionManager): DefaultPipeline = new DefaultPipeline(sessionManager)

  private class DefaultMiddleware extends Middleware {
    override def apply(ctx: Context): Future[Context] = ctx.next()
  }

  private class DefaultContext(var prevCtx: DefaultContext,
                               var nextCtx: DefaultContext,
                               val middleware: Middleware,
                               val name: String,
                               val pipeline: DefaultPipeline,
                               val request: Request,
                               val response: Response) extends Context {

    override def session: Option[Session] = pipeline.session(this)

    override def request_=(req: Request): Unit = {
      copy(req = req)
    }

    override def response_=(resp: Response): Unit = {
      copy(resp = resp)
    }

    def message(req: Request, resp: Response): DefaultContext = {
      copy(req = req, resp = resp)
    }

    private def copy(req: Request = request, resp: Response = response): DefaultContext = {
      new DefaultContext(prevCtx, nextCtx, middleware, name, pipeline, req, resp)
    }

    override def next(): Future[Context] = {
      if (nextCtx != null) {
        nextCtx.message(request, response).received()
      } else Future.successful(this)
    }

    def received(): Future[Context] = {
      middleware(this)
    }
  }

}