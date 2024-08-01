package com.ihcl.hudiniaggregator.model

data class ConfigParameters(
    val bookingDevUrl: String,
    val sebBookingURL: String,
    val socialMediaUrl: String,
    val rateAgentPromoUrl: String,
    val hudiniAuthEmail: String,
    val hudiniAuthPass: String,
    val redisKey: String,
    val redisHost: String,
    val redisPort: String,
    val brandName: String,
    val requestTimeoutMillis: String,
    val sebUserName: String,
    val sebPassword: String,
    val sebDomain: String,
    val memberTierChambers: String,
    val chamberRateFilter: String
)