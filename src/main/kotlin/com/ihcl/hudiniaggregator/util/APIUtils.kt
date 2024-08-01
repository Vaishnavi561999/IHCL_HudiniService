package com.ihcl.hudiniaggregator.util

import com.ihcl.hudiniaggregator.exceptions.HttpResponseException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.reflect.*
import java.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

suspend fun <T : Any> ApplicationCall.fireHttpResponse(statusCode: HttpStatusCode, data: T) {
    respond(statusCode, APIResponse(request.path(), Date(), statusCode, data))
}

private data class APIResponse<T : Any>(
    val path: String, val timestamp: Date, val statusCode: HttpStatusCode, val data: T
)
fun validateDateFormat(startDate: String, endDate: String) {
    if(startDate.isNullOrEmpty() || endDate.isNullOrEmpty()){
        throw HttpResponseException("Mandatory fields are missing", HttpStatusCode.BadRequest)
    }
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    try {
        val parsedStartDate = LocalDate.parse(startDate, formatter)
        val parsedEndDate = LocalDate.parse(endDate, formatter)
        if(!parsedStartDate.isBefore(parsedEndDate)){
            throw HttpResponseException("Invalid start date and endDate", HttpStatusCode.BadRequest)
        }
    } catch (e: DateTimeParseException) {
        throw HttpResponseException("Invalid date format", HttpStatusCode.BadRequest)
    }
}
