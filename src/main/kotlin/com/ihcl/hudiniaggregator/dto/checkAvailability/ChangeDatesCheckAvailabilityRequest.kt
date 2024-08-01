package com.ihcl.hudiniaggregator.dto.checkAvailability

import kotlinx.serialization.Serializable

data class ChangeDatesCheckAvailabilityRequest(
    val hotelId:String,
    val checkInDate:String,
    val checkOutDate:String,
    val rooms:List<ChangeDatesRoomsRequest>
)
@Serializable
data class ChangeDatesRoomsRequest(
    val roomNumber:Int,
    val rateCode:String,
    val adults:String,
    val children:String
)