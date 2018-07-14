package goa

import goa.annotation.RouteParam
import goa.marshalling.MessageBodyWriter
import goa.matcher.PathMatcher

import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe._

private class RouteMiddleware(mapper: MessageBodyWriter, app: Application, pathMatcher: PathMatcher) extends Middleware {

  override def apply(ctx: Context): Unit = {
    val r = findMatchedRouter(request)
    if (r.isDefined) {
      runRouterAction(r.get, request, response, pathMatcher)
    }
  }

  private def findMatchedRouter(request: Request): Option[Route] = {
    val urlMatched = app.routers.filter(r => pathMatcher.tryMatch(r.path, request.path))
    if (urlMatched.isEmpty) {
      response.status = Status.NotFound
      return null
    }
    val methodMatched = urlMatched.filter(r => r.method == request.method)
    if (methodMatched.isEmpty) {
      response.status = Status.MethodNotAllowed
      return null
    }
    val finalMatched = methodMatched.sortWith((x, y) => {
      pathMatcher.getPatternComparator(request.uri).compare(x.path, y.path) > 0
    })
    finalMatched.headOption
  }

  private def reflect(target: Any): InstanceMirror = {
    currentMirror.reflect(target)
  }

  private def runRouterAction(router: Route, request: Request, response: Response, pathMatcher: PathMatcher): Unit = {
    val requestWithRouter = new RequestWithRouterParam(request, router, pathMatcher)
    Goa.putMessage(requestWithRouter -> response)
    router.target match {
      case Some(t) =>
        t match {
          case _: Controller =>
            val any = router.action.asInstanceOf[() => Any]()
            response.body = mapper.write(response, any)
          case _ =>
            val ppp = router.params.map(x => {
              val paramName = x.name
              val t = x.info
              paramName match {
                case Some(p) =>
                  t match {
                    case m if m <:< typeOf[Long] =>
                      requestWithRouter.params.get(p).getOrElse("0").toLong
                    case m if m <:< typeOf[String] =>
                      requestWithRouter.params.get(p).getOrElse("")
                    case _ => throw new IllegalStateException()
                  }
                case None =>
                  t match {
                    case m if m <:< typeOf[Long] => 0L
                    case m if m <:< typeOf[String] => ""
                    case _ => throw new IllegalStateException()
                  }
              }
            })
            val target = reflect(router.target.get)
            val action = router.action.asInstanceOf[MethodSymbol]
            response.body = mapper.write(response, target.reflectMethod(action)(ppp: _*))
        }
      case None =>

    }
  }

}
