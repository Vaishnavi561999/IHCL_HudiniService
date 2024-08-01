package com.ihcl.hudiniaggregator.exceptions

import io.ktor.http.*

data class InternalServerException(
    override val message: String?
):Exception()
class HttpResponseException(val data: Any, val statusCode: HttpStatusCode) : Exception()