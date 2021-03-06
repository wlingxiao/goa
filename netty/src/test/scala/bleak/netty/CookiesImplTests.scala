package bleak
package netty

import io.netty.handler.codec.http.{DefaultHttpHeaders, HttpHeaderNames}
import org.scalatest.{BeforeAndAfter, FunSuite, Matchers}
import scala.collection.JavaConverters._

class CookiesImplTests extends FunSuite with Matchers with BeforeAndAfter {

  test("cookie operation for request") {
    val httpHeaders = new DefaultHttpHeaders()
    httpHeaders.add(HttpHeaderNames.COOKIE, "foo=bar;hello=world")

    val cookiesImpl = new CookiesImpl(httpHeaders, true)
    cookiesImpl.get("foo").get.value shouldEqual "bar"
    cookiesImpl.getAll("hello").map(_.value) shouldEqual Seq("world")

    cookiesImpl -= "foo"
    cookiesImpl.get("foo") shouldEqual None
    httpHeaders.getAsString(HttpHeaderNames.COOKIE) should not include "foo=bar"

    cookiesImpl.add(Cookie("aaa", "123"))
    cookiesImpl.get("aaa").get.value shouldEqual "123"
    httpHeaders.getAsString(HttpHeaderNames.COOKIE) should include("aaa=123")

    cookiesImpl += "xyz" -> Cookie("xyz", "666")
    cookiesImpl.get("xyz").get.value shouldEqual "666"
    httpHeaders.getAsString(HttpHeaderNames.COOKIE) should include("xyz=666")
  }

  test("cookie from response header") {
    val httpHeaders = new DefaultHttpHeaders()
    httpHeaders.add(HttpHeaderNames.SET_COOKIE, "foo=bar")
    httpHeaders.add(HttpHeaderNames.SET_COOKIE, "hello=world")
    httpHeaders.add(HttpHeaderNames.SET_COOKIE, "hello=world2")

    val cookiesImpl = new CookiesImpl(httpHeaders, false)
    cookiesImpl.get("foo").get.value shouldEqual "bar"
    cookiesImpl.getAll("hello").map(_.value).sorted shouldEqual Seq("world", "world2")

    cookiesImpl -= "foo"
    cookiesImpl.get("foo") shouldEqual None
    cookiesImpl -= "hello"
    cookiesImpl.getAll("hello").isEmpty should be(true)

    cookiesImpl.add(Cookie("aaa", "123", httpOnly = true))
    cookiesImpl.get("aaa").get.value shouldEqual "123"
    httpHeaders.getAllAsString(HttpHeaderNames.SET_COOKIE).asScala.contains("aaa=123; HTTPOnly") shouldBe true

    cookiesImpl += "xyz" -> Cookie("xyz", "666")
    cookiesImpl.get("xyz").get.value shouldEqual "666"
    httpHeaders.getAllAsString(HttpHeaderNames.SET_COOKIE).asScala.contains("xyz=666") shouldBe true
  }

  test("cookie iterator for request") {
    val httpHeaders = new DefaultHttpHeaders()
    httpHeaders.add(HttpHeaderNames.COOKIE, "foo=bar;hello=world")
    val cookiesImpl = new CookiesImpl(httpHeaders, true)
    cookiesImpl.iterator.map(c => c._2.name -> c._2.value).toSeq shouldEqual Seq("foo" -> "bar", "hello" -> "world")
  }

  test("cookie iterator for response") {
    val httpHeaders = new DefaultHttpHeaders()
    httpHeaders.add(HttpHeaderNames.SET_COOKIE, "foo=bar")
    httpHeaders.add(HttpHeaderNames.SET_COOKIE, "hello=world")
    val cookiesImpl = new CookiesImpl(httpHeaders, false)
    cookiesImpl.iterator.map(c => c._2.name -> c._2.value).toSeq shouldEqual Seq("foo" -> "bar", "hello" -> "world")
  }

  test("invalid cookie name for request") {
    val httpHeaders = new DefaultHttpHeaders()
    httpHeaders.add(HttpHeaderNames.COOKIE, "fo;o=bar;he:llo=world")
    val cookiesImpl = new CookiesImpl(httpHeaders, true)
    cookiesImpl.get("fo;o") shouldBe None

    assertThrows[IllegalArgumentException] {
      cookiesImpl.add(Cookie("xy;z", "6666"))
    }
  }

  test("invalid cookie name") {
    val httpHeaders = new DefaultHttpHeaders()
    httpHeaders.add(HttpHeaderNames.SET_COOKIE, "fo;o=bar")
    val cookiesImpl = new CookiesImpl(httpHeaders, false)
    cookiesImpl.get("fo;o") shouldBe None

    assertThrows[IllegalArgumentException] {
      cookiesImpl.add(Cookie("xy;z", "6666"))
    }
  }

  test("empty cookie from request") {
    val httpHeaders = new DefaultHttpHeaders()
    val cookiesImpl = new CookiesImpl(httpHeaders, true)
    cookiesImpl.get("foo") shouldBe None
    cookiesImpl.iterator.hasNext shouldBe false
  }

}
