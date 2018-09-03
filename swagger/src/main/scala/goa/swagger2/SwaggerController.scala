package goa.swagger2

import goa._
import org.apache.commons.io.IOUtils

class SwaggerController extends Controller {

  get("/swagger-ui/**") { ctx =>
    val BasePath = "META-INF/resources/webjars/swagger-ui/2.2.10-1/"
    val request = ctx.request
    request.params.get("splat") match {
      case Some(p) =>
        val fileName = BasePath + p
        val format = if (fileName.endsWith("html")) {
          "text/html"
        } else if (fileName.endsWith("css")) {
          "text/css"
        } else if (fileName.endsWith("js")) {
          "text/javascript"
        } else if (fileName.endsWith("png")) {
          "img/png"
        } else {
          "text/plain"
        }
        val file = getClass.getClassLoader.getResourceAsStream(fileName)
        val byte = IOUtils.toByteArray(file)
        ctx.ok()
            .contentType(format)
            .contentLength(byte.length)
            .body(Buf(byte))
      case None =>
        val fileName = BasePath + "index.html"
        val file = getClass.getClassLoader.getResourceAsStream(fileName)
        val byte = IOUtils.toByteArray(file)
        ctx.ok()
            .contentType("text/html")
            .contentLength(byte.length)
            .body(Buf(byte))

    }
  }

  get("/api-docs") { ctx =>
    val swagger = convertToSwagger()
    ctx.ok()
        .contentType(MediaType.Json)
        .body(json(swagger))
  }

}