package com.ihcl.hudiniaggregator.dto.checkAvailability

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class CheckAvailabilityResponse(
    val data: Data?,
    val errors: List<Errors?>
)
@Serializable
data class Errors(
    val message:String?,
    val path:List<String?>?
)
@Serializable
data class Data(
    val getHotelAvailability: GetHotelAvailability?
)
@Serializable
data class GetHotelAvailability(
    val errorCode: String?,
    val roomAvailability: RoomAvailability?,
    val rateFilterMap: List<RateFilterMap?>?,
    val message: String?,
    val warning:List<Warning?>?,
    val contentLists: ContentLists?
)
@Serializable
data class Warning(
    val code:String?,
    val value:String?
)
@Serializable
data class RateFilterMap(
    val code: String?,
    val name: String?,
    val description: String?,
    val rateCodes: String?,
    val rateCodeDescription: String?,
    val rateCodeMapping: List<RateCodeMapping>?,
)
@Serializable
data class RateCodeMapping(
    val key: String?,
    val value: String?
)
@Serializable
data class ContentLists(
    val roomList: List<RoomLists>?,
    val rateList: List<RateList>?,
    val policyList:PolicyList?,
    val chargeList:List<ChargeList?>?
)
@Serializable
data class ChargeList(
    val code:String?,
    val level:String?,
    val name:String?,
    val type:String?,
    val details:ChargeListDetails?
)
@Serializable
data class ChargeListDetails(
    val description:String?,
    val frequency:String?,
    val chargePer:String?,
    val taxAmount:String?,
    val factorType:String?,
    val isInclusive:Boolean?,
    val sortOrder:Int?
)

@Serializable
data class RoomLists(
    val categoryCode: String?,
    val code: String?,
    val name: String?,
    val details: Details?
)
@Serializable
data class RateListDetails(
    val description: String?,
    val detailedDescription: String?,
    val displayName: String?,
    val displayDescription: String?,
    val rateClass: String?,
    val indicators: RoomListDetailsIndicators?,
    val channelAccessOverridesList: List<String>?
)
@Serializable
data class RoomListDetailsIndicators(
    val preferred: Boolean?,
    val breakfastIncluded: Boolean?,
    val commissionable: Boolean?
)
@Serializable
data class Details(
    val description: String?,
    val detailedDescription: String?,
    val viewList: List<ViewList>?,
    val size: Size?,
    val indicators: Indicators?,
    val guestLimit: GuestLimit?,
    val featureList: List<FeatureList>?,
    val extraBed: ExtraBed?,
    @SerializedName("class")
    val detailsClass: DetailsClass?,
    val bedding: List<Bedding>?,
)
@Serializable
data class ViewList(
    val description: String?,
    val code: String?,
    val isgsdPreferred: Boolean?,
    val otaType: String?
)
@Serializable
data class Size(
    val max: Int?,
    val min: Int?,
    val units: String?
)
@Serializable
data class Indicators(
    val preferred: Boolean?
)
@Serializable
data class GuestLimit(
    val adults: Int?,
    val children: Int?,
    val childrenIncluded: Boolean?,
    val guestLimitTotal: Int?,
    val value: Int?
)
@Serializable
data class FeatureList(
    val description: String?,
    val id: String?,
    val otaCode: String?,
    val otaType: String?,
    val sortOrder: Int?
)
@Serializable
data class ExtraBed(
    val allowed: Boolean?,
    val cost: Int?
)
@Serializable
data class DetailsClass(
    val code: String?,
    val description: String?
)
@Serializable
data class Bedding(
    val description: String?,
    val code: String?,
    val quantity: String?,
    val type: String?,
    val isPrimary: Boolean?
)
@Serializable
data class RateList(
    val categoryCode: String?,
    val code: String?,
    val currencyCode: String?,
    val name: String?,
    val details: RateListDetails?
)

@Serializable
data class RoomAvailability(
    val hotel: Hotel?,
    val leastRestrictiveFailure:LeastRestrictiveFailure?,
    val productResult: String?,
    val roomTypes: List<HudiniRoomType>?
)
@Serializable
data class LeastRestrictiveFailure(
    val additionalInformation:String?,
    val productStatus:String?,
    val date:String?,
    val level:String?
)
@Serializable
data class Hotel(
    val id: Int?
)
@Serializable
data class HudiniRoomType(
    val couponApplies:Boolean?,
    val available: Boolean?,
    val availableInventory: Int?,
    val isMaximumPricedItem: Boolean?,
    val isMinimumPricedItem: Boolean?,
    val product: Product?,
    val sortSequenceNumber: Int?
)
@Serializable
data class Product(
    val bookingPolicy: BookingPolicy?,
    val cancelPolicy: BookingPolicy?,
    val prices: Prices?,
    val rate: Rate?,
    val room: Room?,
    val stayLimits: StayLimits?,
    val ref: String?,
    val refValue: String?
)
@Serializable
data class BookingPolicy(
    val code: String?
)
@Serializable
data class Prices(
    val daily: List<Daily>?,
    val perNight: PerNight?,
    val taxesFeesIncluded: Boolean?,
    val totalPrice: TotalPrice?
)
@Serializable
data class Rate(
    val code: String?
)
@Serializable
data class Room(
    val code: String?
)
@Serializable
data class StayLimits(
    val maxStayThru: Int?,
    val maximumStay: Int?,
    val minimumStay: Int?
)
@Serializable
data class Daily(
    val availableInventory: Int?,
    val date: String?,
    val price: Price?
)
@Serializable
data class PerNight(
    val price: HudiniPrice?
)
@Serializable
data class TotalPrice(
    val price: HudiniResPrice?
)
@Serializable
data class HudiniPrice(
    val amount: Double?,
    val currencyCode: String?,
    val fees: Fees?,
    val tax: HudiniTax?,
    val total: HudiniTotal?
)
@Serializable
data class HudiniTax(
    val amount: Double?
)
@Serializable
data class HudiniTotal(
    val amount: Double?,
    val amountWithInclusiveTaxes: Double?,
    val amountWithTaxesFees: Double?,
    val amountWithFees: Double?
)
@Serializable
data class Fees(
    val amount: Double?,
    val breakDown: List<BreakDown?>?
)
@Serializable
data class HudiniResPrice(
    val amount: Double?,
    val currencyCode: String?,
    val fees: Fee?,
    val tax: Taxes?,
    val total: Totals?
)
@Serializable
data class Totals(
    val amount: Double?,
    val amountPayableNow: Double?,
    val amountWithInclusiveTaxes: Double?,
    val amountWithTaxesFees: Double?,
    val amountPayAtProperty: Double?,
    val amountWithFees: Double?
)
@Serializable
data class Fee(
    val amount: Double?,
    val breakDown: List<BreakDown?>?
)
@Serializable
data class Price(
    val amount: Double?,
    val currencyCode: String?,
    val fees: Fees?,
    val tax: Tax?,
    val total: Total?
)
@Serializable
data class Tax(
    val amount: Double?,
    val breakDown: List<BreakDown?>?
)
@Serializable
data class Total(
    val amount: Double?,
    val amountPayAtProperty: Double?,
    val amountPayableNow: Double?,
    val amountWithInclusiveTaxes: Double?,
    val amountWithTaxesFees: Double?,
    val amountWithFees: Double?
)
@Serializable
data class BreakDown(
    val amount: Double?,
    val code: String?,
    val isInclusive: Boolean?,
    val isPayAtProperty: Boolean?,
    val isPerStay: Boolean?
)
@Serializable
data class Taxes(
    val breakDown: List<BreakDown?>?,
    val amount: Double?
)
@Serializable
data class PolicyList(
    val bookingPolicy: List<ContentBookingPolicy?>?,
    val cancelPolicy: List<ContentCancelPolicy?>?,
    val commisionPolicy: List<ContentCommisionPolicy?>?
)
@Serializable
data class ContentCommisionPolicy(
    val code:String?,
    val description:String?,
    val commission:Commission?
)
@Serializable
data class Commission(
    val value:Double?,
    val unitType:String?,
    val calculationType:String?
)
@Serializable
data class ContentBookingPolicy(
    val code: String?,
    val allowPay: Boolean?,
    val depositFee: DepositFee?,
    val description: String?,
    val guaranteeLevel: String?,
    val holdTime: String?,
    val refundableStay: String?,
    val requirements: List<String?>?,
    val transactionFeeDisclaimer: String?
)
@Serializable
data class ContentCancelPolicy(
    val code: String?,
    val cancelFeeAmount: CancelFeeAmount?,
    val cancelFeeType: String?,
    val cancelPenaltyDate: String?,
    val cancelTime: String?,
    val cancelTimeIn: Int?,
    val chargeThreshold: String?,
    val chargeType: String?,
    val charges: List<String?>?,
    val description: String?,
    val lateCancellationPermitted: Boolean?,
    val modificationRestrictions: String?,
    val noShowFeeAmount: NoShowFeeAmount?,
    val noShowFeeType: String?
)
@Serializable
data class DepositFee(
    val amount: Int?,
    val dueDays: Int?,
    val dueTime: String?,
    val dueType: String?,
    val isPrePayment: Boolean?,
    val taxInclusive: Boolean?,
    val type: String?
)
@Serializable
data class CancelFeeAmount(
    val taxInclusive: Boolean,
    val value: Int
)
@Serializable
data class NoShowFeeAmount(
    val taxInclusive: String,
    val value: Int
)