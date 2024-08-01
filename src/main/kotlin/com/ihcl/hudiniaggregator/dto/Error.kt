package com.ihcl.hudiniaggregator.dto

import kotlinx.serialization.Serializable

@Serializable
data class Error(
    val header: String?,
    val message: String?,
    val name: String?

)