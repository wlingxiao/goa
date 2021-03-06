package bleak

import java.util.Base64

import scala.concurrent.Future

class BasicAuthMiddleware(username: String, password: String) extends Middleware {
  override def apply(ctx: Context): Future[Context] = {
    if (allow(ctx)) {
      return ctx.next()
    }
    val request = ctx.request
    request.headers.get(Fields.Authorization) match {
      case Some(auth) =>
        val authToken = auth.split(" ")
        if (authToken.length == 2) {
          val token = new String(Base64.getDecoder.decode(authToken(1)))
          val usernameAndPassword = token.split(":")
          if (usernameAndPassword.length == 2) {
            val authUser = usernameAndPassword(0)
            val authPassword = usernameAndPassword(1)
            if (validate(ctx, authUser, authPassword)) {
              return ctx.next()
            }
          }
        }
        authFailed(ctx)
      case None => authFailed(ctx)
    }
  }

  protected def allow(ctx: Context): Boolean = {
    false
  }

  protected def validate(ctx: Context, tokenUsername: String, tokenPassword: String): Boolean = {
    tokenUsername == username && tokenPassword == password
  }

  private def authFailed(ctx: Context): Future[Context] = {
    val response = ctx.response
    response.status = Status.Unauthorized
    response.headers.set(Fields.WwwAuthenticate, """Basic realm="input username and password"""")
    ctx.response = response
    Future.successful(ctx)
  }

}
