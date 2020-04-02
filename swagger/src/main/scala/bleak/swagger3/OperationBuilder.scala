package bleak.swagger3

import io.swagger.v3.oas.models.{OpenAPI, Operation, Paths}
import io.swagger.v3.oas.models.media.{Content, MediaType}
import io.swagger.v3.oas.models.parameters.{Parameter, RequestBody}
import io.swagger.v3.oas.models.responses.{ApiResponse, ApiResponses}

class OperationBuilder(api: OpenAPI, op: Operation) {

  def query(name: String, desc: Option[String]): ParamBuilder = {
    val parameter = new Parameter
    parameter.setName(name)
    parameter.setIn("query")
    // parameter.setSchema(s)
    parameter.setDescription(desc.orNull)
    op.addParametersItem(parameter)
    new ParamBuilder(api, op)
  }

  def requestBody(desc: String, mimeType: Iterable[String]): RequestBodyBuilder = {
    val requestBody = new RequestBody
    val content = new Content
    mimeType.map { mt =>
      val m = new MediaType
      // m.setSchema()
      content.addMediaType(mt, m)
    }
    requestBody.setContent(content)
    op.setRequestBody(requestBody)
    new RequestBodyBuilder(api, op)
  }

  def response(name: String, desc: String, mimeType: String): ResponseBuilder = {
    val res = new ApiResponse
    res.setDescription(desc)
    val mediaType = new MediaType
    // mediaType.setSchema()
    val content = new Content
    content.addMediaType(mimeType, mediaType)
    op.getResponses.addApiResponse(name, res)
    new ResponseBuilder(api, op)
  }

}
