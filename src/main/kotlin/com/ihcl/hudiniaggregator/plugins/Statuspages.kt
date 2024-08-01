package com.ihcl.hudiniaggregator.plugins


import com.ihcl.hudiniaggregator.exceptions.ErrorResponseDTO
import com.ihcl.hudiniaggregator.exceptions.HttpResponseException
import com.ihcl.hudiniaggregator.exceptions.InternalServerException
import com.ihcl.hudiniaggregator.util.fireHttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory

fun Application.statusPages() {
    val log = LoggerFactory.getLogger(javaClass)
    install(StatusPages) {
        exception<InternalServerException>{ call, cause ->
            log.error("Exception occurred while calling api"+cause.stackTraceToString())
            call.respond(HttpStatusCode.InternalServerError,ErrorResponseDTO(cause.message!!))
        }
        exception<HttpResponseException> { call, cause ->
            log.error("error {} ",cause.message)
            cause.printStackTrace()
            call.fireHttpResponse(cause.statusCode, cause.data)
        }
    }
}