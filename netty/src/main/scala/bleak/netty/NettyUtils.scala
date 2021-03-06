package bleak
package netty

import io.netty.handler.codec.http.cookie

private object NettyUtils {

  def nettyCookieToCookie(nettyCookie: cookie.Cookie): bleak.Cookie = {
    bleak.Cookie(nettyCookie.name(),
      nettyCookie.value(),
      nettyCookie.domain(),
      nettyCookie.path(),
      nettyCookie.maxAge(),
      nettyCookie.isSecure,
      nettyCookie.isHttpOnly)
  }

  def cookieToNettyCookie(goaCookie: bleak.Cookie): cookie.Cookie = {
    val nettyCookie = new cookie.DefaultCookie(goaCookie.name, goaCookie.value)
    nettyCookie.setDomain(goaCookie.domain)
    nettyCookie.setPath(goaCookie.path)
    nettyCookie.setMaxAge(goaCookie.maxAge)
    nettyCookie.setSecure(goaCookie.secure)
    nettyCookie.setHttpOnly(goaCookie.httpOnly)
    nettyCookie
  }

}
