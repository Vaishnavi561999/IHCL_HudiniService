package com.ihcl.hudiniaggregator.plugins

import io.ktor.serialization.gson.*
import io.ktor.server.application.Application
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.application.install
import io.ktor.server.routing.get
import io.ktor.server.application.call

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson {
            serializeNulls()
        }
    }
    routing {
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}
