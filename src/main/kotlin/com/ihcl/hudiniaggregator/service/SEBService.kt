package com.ihcl.hudiniaggregator.service

import com.ihcl.hudiniaggregator.config.PropertiesConfiguration
import com.ihcl.hudiniaggregator.dto.RequestToSEB
import com.ihcl.hudiniaggregator.dto.SEBRequest
import com.ihcl.hudiniaggregator.dto.SEBResponse
import com.ihcl.hudiniaggregator.exceptions.HttpResponseException
import com.ihcl.hudiniaggregator.exceptions.InternalServerException
import com.ihcl.hudiniaggregator.plugins.ConfigureHTTPClient
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.litote.kmongo.json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SEBService {
    private val prop = PropertiesConfiguration.env
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    suspend fun updateEmployeeDetails(request: SEBRequest): SEBResponse {
        log.info("SEB request ${request.json}")
        val body = RequestToSEB(
            userName = prop.sebUserName,
            password = prop.sebPassword,
            domain = prop.sebDomain,
            myTajREQID = request.myTajREQID,
            startDate = request.startDate,
            endDate = request.endDate,
            approvedRooms = request.approvedRooms,
            crsReferenceNumber = request.crsReferenceNumber,
            crsTimeStamp = request.crsTimeStamp,
            emailID = request.emailID,
            mobileNumber = request.mobileNumber,
            numberOfPerson = request.numberOfPerson,
            crsRequestID = request.crsRequestID,
            hotelName = request.hotelName,
        )
        log.info("Request prepared to SEB ${body.json}")

        try {
            val response: HttpResponse =
                ConfigureHTTPClient.client.post(prop.sebBookingURL) {
                    timeout {
                        requestTimeoutMillis = prop.requestTimeoutMillis.toLong()
                    }
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            log.info("SEB Response: ${response.bodyAsText()}")
            if (response.status.isSuccess()) {
                return response.body<SEBResponse>()
            }else
                throw HttpResponseException(response.bodyAsText(), response.status)
        } catch (e: Exception) {
            log.error("Exception occurred while calling SEB API is ${e.message} due to ${e.stackTrace} cause: ${e.cause}")
            throw InternalServerException(e.message)
        }
    }
}