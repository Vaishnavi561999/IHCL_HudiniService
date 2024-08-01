package com.ihcl.hudiniaggregator.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*

fun Application.corsConfig() {
    install(CORS){
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }
    install(DefaultHeaders){
        header("Content-Security-Policy", "script-src ‘self’;")
        header("X-Content-Type-Options", "nosniff")
        header("X-XSS-Protection", "1")
    }
}