package com.ihcl.hudiniaggregator.util

import com.ihcl.hudiniaggregator.config.PropertiesConfiguration
import com.ihcl.hudiniaggregator.config.RedisConfig
import com.ihcl.hudiniaggregator.dto.HudiniGetTokenResponse
import com.ihcl.hudiniaggregator.exceptions.InternalServerException
import com.ihcl.hudiniaggregator.plugins.ConfigureHTTPClient
import com.ihcl.hudiniaggregator.service.CreateBookingService
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.koin.java.KoinJavaComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GenerateToken {
    private val prop = PropertiesConfiguration.env
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val createBookingService by KoinJavaComponent.inject<CreateBookingService>(CreateBookingService::class.java)

    suspend fun generateTokenAndSave():String{
        log.info("Generating token...")
        val mutation = """
            mutation {
                auth{
                    getToken(
                        email: "${prop.hudiniAuthEmail}",
                        password: "${prop.hudiniAuthPass}"
                    )
                }
            }
        """.trimIndent()
        log.debug("GraphQl mutation prepared for authentication is $mutation")
        val jsonQuery = createBookingService.convertGraphQueryToJSONBody(mutation)
        log.debug("converted mutation to json $jsonQuery")
        try {
            val response: HttpResponse =
                ConfigureHTTPClient.client.post(prop.bookingDevUrl) {
                    timeout {
                        requestTimeoutMillis = prop.requestTimeoutMillis.toLong()
                    }
                    contentType(ContentType.Application.Json)
                    setBody(jsonQuery)
                }
            log.debug("Token generation response ${response.bodyAsText()}")
            if(response.status == HttpStatusCode.OK) {
                val res = response.body<HudiniGetTokenResponse>()
                val token = "${Constants.BEARER} ${res.data.auth.getToken.accessToken}"
                RedisConfig.setKey(Constants.HUDINITOKEN, token)
                return token
            }else{
                throw InternalServerException("Unknown Response from authorization API")
            }
        } catch (e: Exception) {
            log.error("Exception occurred while calling api is ${e.message} due to ${e.cause}")
            throw InternalServerException(e.message)
        }
    }
    suspend fun getToken():String{
        var token = RedisConfig.getKey(Constants.HUDINITOKEN)
        if(token.isNullOrEmpty()){
            token = generateTokenAndSave()
        }
        return token
    }
}