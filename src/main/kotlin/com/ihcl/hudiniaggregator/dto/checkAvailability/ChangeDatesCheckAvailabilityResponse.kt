package com.ihcl.hudiniaggregator.dto.checkAvailability

import com.ihcl.hudiniaggregator.dto.hotelAvailability.FETax

data class ChangeDatesCheckAvailabilityResponse(
    val hotelId: String?,
    var roomTypes: MutableList<ChangeDatesRateCodePromoCodeRoomTypes>?,
)
data class ChangeDatesRateCodePromoCodeRoomTypes(
    val roomNumber: Int?,
    val rooms: MutableList<ChangeDatesRateCodePromoCodeRooms?>?,
    val chargeList:List<ChargeList?>?,
    var leastRestrictiveFailure: LeastRestrictiveFailure?,
    )
data class ChangeDatesRateCodePromoCodeRooms(
    val available:Boolean?,
    val availableInventory:Int?,
    val roomCode:String?,
    val rateCode: String?,
    val rateContent: RateList?,
    val bookingPolicy: ContentBookingPolicy?,
    val cancellationPolicy: ContentCancelPolicy?,
    val daily: List<Daily>?,
    val perNight: PerNight?,
    val tax: FETax?,
    val total: Total?,
)