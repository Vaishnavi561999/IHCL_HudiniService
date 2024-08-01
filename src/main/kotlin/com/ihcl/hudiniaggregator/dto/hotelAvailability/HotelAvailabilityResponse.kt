package com.ihcl.hudiniaggregator.dto.hotelAvailability

import com.ihcl.hudiniaggregator.dto.checkAvailability.*

//This is the front end restructured response dto
data class HotelAvailabilityResponse(
    val tab: MutableSet<String?>?,
    val hotelId: String?,
    val synxisId:Int?,
    val couponCode: String?,
    var isCouponCodeValid: Boolean?,
    var couponRemark: String?,
    val roomAvailability: Rates?,
    val chargeList:List<ChargeList?>?,
    val leastRestrictiveFailure: LeastRestrictiveFailure?,
    val errorCode: String?,
    val message: String?
)

data class Rates(
    var roomRates: MutableList<RoomTypes?>?,
    var packagesRates: MutableList<PackageRoomTypes?>?,
    var memberExclusiveRates: MutableList<PackageRoomTypes?>?,
    var offerRates: MutableList<PackageRoomTypes?>?,
    var promotionRates: MutableList<PackageRoomTypes?>?
)

data class PackageRoomTypes(
    val roomCode: String?,
    var rooms: MutableList<PackageRooms?>?,
)

data class PackageRooms(
    val rateCode: String?,
    val available: Boolean?,
    val isCommissionable:Boolean?,
    val availableInventory: Int?,
    val daily: List<Daily>?,
    val perNight: PerNight?,
    val tax: Tax?,
    val total: Total?,
    val rateContent: RateList?,
    val bookingPolicy: ContentBookingPolicy?,
    val cancellationPolicy: ContentCancelPolicy?,
    val commisionPolicy: ContentCommisionPolicy?,
    )

data class RoomTypes(
    val roomCode: String?,
    var rooms: MutableList<Rooms?>?,
)

data class Rooms(
    val rateCode: String?,
    val isCommissionable: Boolean?,
    var rateContent: RateList?,
    var bookingPolicy: ContentBookingPolicy?,
    var cancellationPolicy: ContentCancelPolicy?,
    var commisionPolicy: ContentCommisionPolicy?,
    var standardRate: StandardRate?,
    var memberRate: StandardRate?
)

data class StandardRate(
    val available: Boolean?,
    val availableInventory: Int?,
    val rateCode: String?,
    val bookingPolicyCode : String?,
    val cancellationPolicyCode: String?,
    val daily: List<Daily>?,
    val perNight: PerNight?,
    val tax: FETax?,
    val total: Total?,
)

data class FETax(
    val amount: Double?,
    val breakDown: List<FEBreakDown?>?
)

data class FEBreakDown(
    val amount: Double?,
    val code: String?,
    val isInclusive: Boolean?,
    val isPayAtProperty: Boolean?,
    val isPerStay: Boolean?
)