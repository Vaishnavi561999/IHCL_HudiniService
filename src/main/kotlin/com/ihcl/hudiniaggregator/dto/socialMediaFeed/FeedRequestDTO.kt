package com.ihcl.hudiniaggregator.dto.socialMediaFeed

import kotlinx.serialization.Serializable
@Serializable
data class FeedRequestDTO(
    val brands: List<Brand?>?,
    val categoryID: Int?,
    val startDateEpoch:Int?,
    val endDateEpoch:Int?,
    val noOfRows:Int?

)