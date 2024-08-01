package com.ihcl.hudiniaggregator.config

import com.ihcl.hudiniaggregator.model.ConfigParameters
import io.ktor.server.application.*

object PropertiesConfiguration {
    lateinit var env: ConfigParameters

    fun initConfig(environment: ApplicationEnvironment) {
        env = ConfigParameters(
            bookingDevUrl = environment.config.property("ktor.api.bookingDevUrl").getString(),
            sebBookingURL = environment.config.property("ktor.api.sebBookingURL").getString(),
            socialMediaUrl = environment.config.property("ktor.api.socialMediaUrl").getString(),
            rateAgentPromoUrl = environment.config.property("ktor.api.rateAgentPromoUrl").getString(),
            hudiniAuthEmail = environment.config.property("ktor.api.hudiniAuthEmail").getString(),
            hudiniAuthPass = environment.config.property("ktor.api.hudiniAuthPass").getString(),
            redisKey = environment.config.property("ktor.api.redisKey").getString(),
            redisHost = environment.config.property("ktor.api.redisHost").getString(),
            redisPort = environment.config.property("ktor.api.redisPort").getString(),
            brandName = environment.config.property("ktor.api.brandName").getString(),
            requestTimeoutMillis = environment.config.property("ktor.api.requestTimeoutMillis").getString(),
            sebUserName = environment.config.property("ktor.api.sebUserName").getString(),
            sebPassword = environment.config.property("ktor.api.sebPassword").getString(),
            sebDomain = environment.config.property("ktor.api.sebDomain").getString(),
            memberTierChambers = environment.config.property("ktor.api.memberTierChambers").getString(),
            chamberRateFilter = environment.config.property("ktor.api.chamberRateFilter").getString()
        )
    }
}