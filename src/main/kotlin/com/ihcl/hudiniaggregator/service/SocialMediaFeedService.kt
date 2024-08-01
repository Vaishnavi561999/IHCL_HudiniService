package com.ihcl.hudiniaggregator.service

import com.google.gson.FieldNamingPolicy.UPPER_CAMEL_CASE
import com.google.gson.GsonBuilder
import com.ihcl.hudiniaggregator.config.PropertiesConfiguration
import com.ihcl.hudiniaggregator.dto.socialMediaFeed.InstagramFeedResponse
import com.ihcl.hudiniaggregator.dto.socialMediaFeed.FeedRequestDTO
import com.ihcl.hudiniaggregator.dto.socialMediaFeed.Brand
import com.ihcl.hudiniaggregator.dto.socialMediaFeed.InstagramFeedRequest
import com.ihcl.hudiniaggregator.dto.socialMediaFeed.FeedResponseDTO
import com.ihcl.hudiniaggregator.dto.socialMediaFeed.ResData
import com.ihcl.hudiniaggregator.exceptions.InternalServerException
import com.ihcl.hudiniaggregator.plugins.ConfigureHTTPClient
import com.ihcl.hudiniaggregator.util.Constants
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.contentType
import io.ktor.http.ContentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SocialMediaFeedService {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val prop = PropertiesConfiguration.env
    private val gson = GsonBuilder().setFieldNamingPolicy(UPPER_CAMEL_CASE).create()!!
    suspend fun readInstagramFeeds(request: FeedRequestDTO): Any {
        log.debug("Request received from front end for social media feed api is {}", request)
        val req = InstagramFeedRequest(
            listOf(
                Brand(
                    request.brands?.get(0)?.brandID)
            ),
            0,
            request.endDateEpoch,
            request.noOfRows,
            0,
            Constants.SOCIAL_MEDIA_FEED_ORDERBY,
            Constants.SOCIAL_MEDIA_FEED_ORDERBYCOLUMN,
            request.startDateEpoch
        )
        log.debug("Request prepared to call social media feed api is ${Json.encodeToString(req)}")
        try {
            val response = ConfigureHTTPClient.client.post(prop.socialMediaUrl) {
                timeout {
                    requestTimeoutMillis = prop.requestTimeoutMillis.toLong()
                }
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(req)
            }
            log.debug("Response received from hudini check availability api is {}", response.bodyAsText())
            val instagramFeedResponse  = gson.fromJson(response.bodyAsText(),InstagramFeedResponse::class.java)
            val data = mutableListOf<ResData>()
            instagramFeedResponse.data?.forEach {
                data.add(
                    ResData(
                        it?.url,
                        it?.attachmentMetadata
                    )
                )
            }
            return FeedResponseDTO(
                instagramFeedResponse.success,
                data
            )
        } catch (e: Exception) {
            log.error("Exception occurred while calling api is ${e.message} due to ${e.cause}")
            throw InternalServerException(e.message)
        }
    }
}