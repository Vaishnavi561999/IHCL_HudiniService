package com.ihcl.hudiniaggregator.plugins

import com.ihcl.hudiniaggregator.route.hudiniRoute
import com.ihcl.hudiniaggregator.route.socialMediaFeed
import io.ktor.server.application.Application

fun Application.configureRouting() {
    hudiniRoute()
    socialMediaFeed()
}
