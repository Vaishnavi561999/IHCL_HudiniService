package com.ihcl.hudiniaggregator.route

import com.ihcl.hudiniaggregator.dto.socialMediaFeed.FeedRequestDTO
import com.ihcl.hudiniaggregator.service.SocialMediaFeedService
import io.ktor.server.application.call
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.routing.route
import io.ktor.server.routing.post


import org.koin.java.KoinJavaComponent

fun Application.socialMediaFeed() {
    val socialMediaFeedService by KoinJavaComponent.inject<SocialMediaFeedService>(SocialMediaFeedService::class.java)
    routing {
        route("/v1") {
            post("/social-media-feed") {
                val request = call.receive<FeedRequestDTO>()
                val response = socialMediaFeedService.readInstagramFeeds(request)
                call.respond(response)
            }
        }
    }
}