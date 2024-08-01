package com.ihcl.hudiniaggregator.plugins

import com.google.gson.Gson
import com.ihcl.hudiniaggregator.config.PropertiesConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import io.ktor.client.engine.okhttp.*
import okhttp3.ConnectionPool
import okhttp3.Protocol
import java.util.concurrent.TimeUnit

object ConfigureHTTPClient {
    val log: Logger = LoggerFactory.getLogger(javaClass)
    val prop = PropertiesConfiguration.env
    val client = HttpClient(OkHttp) {
        engine {

            config {
                // this: OkHttpClient.Builder
                connectionPool(ConnectionPool(100, 5, TimeUnit.MINUTES))
                readTimeout(prop.requestTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
                connectTimeout(prop.requestTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
                writeTimeout(prop.requestTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
                retryOnConnectionFailure(true)
                protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
                followRedirects(true)

            }

        }

        install(ContentNegotiation) {
            gson()
            register(contentType = ContentType.Text.Html,converter =  GsonConverter(Gson()))
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        install(HttpTimeout)
    }
}