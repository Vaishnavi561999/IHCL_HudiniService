package com.ihcl.hudiniaggregator.dto

import kotlinx.serialization.Serializable

@Serializable
data class CancelBookingResponse(
    val data: CancelBookingResponseData?
)
@Serializable
data class CancelBookingResponseData(
    val cancelHotelBooking: CancelHotelBooking?
)
@Serializable
data class CancelHotelBooking(
    val cancellationNumber: String?,
    val errorCode:String?,
    val message: String?
)