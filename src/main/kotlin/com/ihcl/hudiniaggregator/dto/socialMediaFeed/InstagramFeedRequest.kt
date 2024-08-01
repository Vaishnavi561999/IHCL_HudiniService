package com.ihcl.hudiniaggregator.dto.socialMediaFeed

import kotlinx.serialization.Serializable
@Serializable
data class InstagramFeedRequest(
    val brands: List<Brand?>?,
    val categoryID: Int?,
    val endDateEpoch: Int?,
    val noOfRows: Int?,
    val oFFSET: Int?,
    val orderBY: String?,
    val orderBYColumn: String?,
    val startDateEpoch: Int?
)
@Serializable
data class Brand(
    val brandID: Int?


)