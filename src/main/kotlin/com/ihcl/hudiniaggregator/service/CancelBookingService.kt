package com.ihcl.hudiniaggregator.service

import com.ihcl.hudiniaggregator.config.PropertiesConfiguration
import com.ihcl.hudiniaggregator.dto.CancelBookingInput
import com.ihcl.hudiniaggregator.dto.CancelBookingResponse
import com.ihcl.hudiniaggregator.exceptions.InternalServerException
import com.ihcl.hudiniaggregator.plugins.ConfigureHTTPClient
import com.ihcl.hudiniaggregator.util.Constants
import com.ihcl.hudiniaggregator.util.GenerateToken
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import org.koin.java.KoinJavaComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.ktor.client.request.headers
import io.ktor.http.*
import org.litote.kmongo.json

class CancelBookingService {
    private val prop = PropertiesConfiguration.env
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val createBooking by KoinJavaComponent.inject<CreateBookingService>(CreateBookingService::class.java)
    private val generateToken by KoinJavaComponent.inject<GenerateToken>(GenerateToken::class.java)

    /* This method is used to call hudini cancel booking API */
    suspend fun cancelBooking(request: CancelBookingInput): HttpResponse {
        val token = generateToken.getToken()
        val cancelBookingUrl = prop.bookingDevUrl
        log.debug("Request received to call hudini cancel Booking is ${request.json}")
        val mutation = """
            mutation {cancelHotelBooking(cancelBooking: {
                hotelId: "${request.hotelId}",
                  crsConfirmationNumber: "${request.crsConfirmationNumber}",
                  cancellationDetails: {comment: "${request.cancellationReason}"}
                }) 
                {
                  cancellationNumber
                  errorCode
                  message
                }
            }
            """.trimIndent()
        log.debug("_____GraphQl mutation prepared to call hudini cancel booking api_____")
        val jsonQuery = createBooking.convertGraphQueryToJSONBody(mutation)
        try {
            val response: HttpResponse = ConfigureHTTPClient.client.post(cancelBookingUrl) {
                timeout {
                    requestTimeoutMillis = prop.requestTimeoutMillis.toLong()
                }
                headers{
                    append(Constants.AUTHORIZATION,token)
                }
                contentType(ContentType.Application.Json)
                setBody(jsonQuery)
            }
            if(response.status== HttpStatusCode.Forbidden){
                generateToken.generateTokenAndSave()
                return cancelBooking(request)
            }
            log.debug("Response received from hudini cancel booking is {}", response.body<CancelBookingResponse>())
            return response
        } catch (e: Exception) {
            log.error("Exception occurred while calling hudini api is ${e.message} due to ${e.stackTrace}")
            throw InternalServerException(e.message)
        }
    }
}