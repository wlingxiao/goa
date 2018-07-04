package goa.http1

import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZoneOffset}
import java.util.{Date, Locale}

import goa.Cookie

import scala.collection.mutable.ArrayBuffer

private[goa] trait CookieEncoder {

  protected val EQUALS = '='

  protected val SEMICOLON = ';'

  protected val SP = ' '

  protected def addUnquoted(sb: StringBuilder, name: String, value: String): Unit = {
    sb.append(name)
    sb.append(EQUALS)
    sb.append(value)
    sb.append(SEMICOLON)
    sb.append(SP)
  }

  protected def add(sb: StringBuilder, name: String, value: Any): Unit = {
    sb.append(name)
    sb.append(EQUALS)
    sb.append(value)
    sb.append(SEMICOLON)
    sb.append(SP)
  }

  protected def stripTrailingSeparator(buf: StringBuilder): String = {
    if (buf.nonEmpty) buf.setLength(buf.length - 2)
    buf.toString
  }

  protected def stripTrailingSeparatorOrNull(buf: StringBuilder): String = if (buf.isEmpty) null
  else stripTrailingSeparator(buf)

}

private[goa] class ServerCookieEncoder extends CookieEncoder {

  def encode(cookie: Cookie): String = {
    require(cookie != null, "cookie should not be null")
    val buf = StringBuilder.newBuilder
    addUnquoted(buf, cookie.name, cookie.value.getOrElse(""))

    if (cookie.maxAge != Long.MinValue) {
      add(buf, CookieHeaderNames.MAX_AGE, cookie.maxAge)
      val expires = new Date(cookie.maxAge * 1000 + System.currentTimeMillis)
      addUnquoted(buf, CookieHeaderNames.EXPIRES, format(expires))
    }

    cookie.path.foreach(addUnquoted(buf, CookieHeaderNames.PATH, _))

    if (cookie.secure) {
      buf.append(CookieHeaderNames.SECURE)
      buf.append(SEMICOLON)
      buf.append(SP)
    }

    if (cookie.httpOnly) {
      buf.append(CookieHeaderNames.HTTPONLY)
      buf.append(SEMICOLON)
      buf.append(SP)
    }

    stripTrailingSeparator(buf)
  }


  private def format(date: Date): String = {
    val httpDateFormat = DateTimeFormatter
      .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
      .withLocale(Locale.ENGLISH)
      .withZone(ZoneId.of("GMT"))
    date.toInstant.atOffset(ZoneOffset.UTC).format(httpDateFormat)
  }

}

private[goa] class ClientCookieEncoder extends CookieEncoder {

  def encode(cookie: Cookie): String = {
    val buf = StringBuilder.newBuilder
    val value = cookie.value.getOrElse("")
    addUnquoted(buf, cookie.name, value)
    stripTrailingSeparator(buf)
  }

  def encode(cookies: Iterable[Cookie]): String = {
    if (cookies.isEmpty) {
      null
    } else {
      val buf = StringBuilder.newBuilder
      cookies.toArray.sorted(CookieOrdering).foreach(encode(buf, _))
      stripTrailingSeparatorOrNull(buf)
    }
  }

  private def encode(buf: StringBuilder, c: Cookie): Unit = {
    val name = c.name
    val value = c.value

    add(buf, name, value)
  }

  private val CookieOrdering = new Ordering[Cookie]() {
    override def compare(c1: Cookie, c2: Cookie): Int = {
      def pathLen(c: Cookie): Int = c.path.map(_.length).getOrElse(Integer.MAX_VALUE)

      val diff = pathLen(c2) - pathLen(c1)
      if (diff != 0) diff else -1
    }
  }

}