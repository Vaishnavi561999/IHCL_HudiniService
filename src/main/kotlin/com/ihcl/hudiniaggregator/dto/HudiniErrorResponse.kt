package com.ihcl.hudiniaggregator.dto

import kotlinx.serialization.Serializable

@Serializable
data class HudiniErrorResponse(
    val error: Error?
)