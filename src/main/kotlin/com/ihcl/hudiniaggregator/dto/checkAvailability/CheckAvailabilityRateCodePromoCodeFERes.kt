package com.ihcl.hudiniaggregator.dto.checkAvailability

import com.ihcl.hudiniaggregator.dto.hotelAvailability.FETax

data class CheckAvailabilityRateCodePromoCodeFERes(
    val hotelId: String?,
    val synxisId:Int?,
    val roomTypes: MutableList<RateCodePromoCodeRoomTypes>?,
    val couponCode:String?,
    val chargeList:List<ChargeList?>?,
    val leastRestrictiveFailure:LeastRestrictiveFailure?,
    val isCouponCodeValid:Boolean?,
    val couponRemark:String?,
    val message: String?,
    val errorCode: String?
    )
data class RateCodePromoCodeRoomTypes(
    val roomCode: String?,
    val rooms: MutableList<RateCodePromoCodeRooms?>?,
)
data class RateCodePromoCodeRooms(
    val available:Boolean?,
    val availableInventory:Int?,
    val rateCode: String?,
    val rateContent: RateList?,
    val bookingPolicy: ContentBookingPolicy?,
    val cancellationPolicy: ContentCancelPolicy?,
    val daily: List<Daily>?,
    val perNight: PerNight?,
    val tax: FETax?,
    val total: Total?,
)