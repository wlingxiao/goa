package goa.http1

import goa.{BaseTests, Cookie}

class ServerCookieEncoderTest extends BaseTests {

  val encoder = new ServerCookieEncoder

  test("encode cookie") {
    val cookie = Cookie("name", "test", "localhost", "/home", 1000L, secure = true, httpOnly = true)
    val ret = encoder.encode(cookie)
    ret should startWith("name=test; Max-Age=1000; Expires=")
    ret should endWith("GMT; Path=/home; Secure; HTTPOnly")
  }

}
