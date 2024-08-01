package com.ihcl.hudiniaggregator.dto

import kotlinx.serialization.Serializable

@Serializable
data class GetHotelLeadAvailabilityRes(
    val data: Data?,
    val errors: List<Errors?>?
)
@Serializable
data class NonIndainHotelsGetHotelLeadAvailabilityRes(
    val data: NonIndianHotelsData?,
    val errors: List<Errors?>?
)
@Serializable
data class Errors(
    val message:String?
)

@Serializable
data class Hotel(
    val id: Int,
    val hudiniId:String
)

@Serializable
data class GetHotelLeadAvailability(
    val leadAvailability: List<LeadAvailability>,
    val errorCode: String?,
    val message: String?
)
@Serializable
data class NonIndianHotelsGetHotelLeadAvailability(
    val leadAvailability: List<NonIndianHotelsLeadAvailability>,
    val errorCode: String?,
    val message: String?
)

@Serializable
data class Data(
    val getHotelLeadAvailability: GetHotelLeadAvailability?
)

@Serializable
data class NonIndianHotelsData(
    val getHotelLeadAvailability: NonIndianHotelsGetHotelLeadAvailability?
)

@Serializable
data class LeadAvailability(
    val arrivalDate: String,
    var available: Boolean,
    val departureDate: String,
    val hotel: Hotel,
    val price: List<Price?>?,
    val priceGroup: List<PriceGroup?>?,
    val leastRestrictiveFailure: LeastRestrictiveFailure,
    val failures: List<Failures>
)
@Serializable
data class NonIndianHotelsLeadAvailability(
    val arrivalDate: String,
    var available: Boolean,
    val departureDate: String,
    val hotel: Hotel,
    val price: List<NonIndianHotelsPrice?>?,
    val priceGroup: List<NonIndianHotelsPriceGroup>,
    val leastRestrictiveFailure: LeastRestrictiveFailure,
    val failures: List<Failures>
)
@Serializable
data class LeastRestrictiveFailure(
    val level: String?,
    val cause: String?,
    val overrideAllowed: Boolean?
)

@Serializable
data class Failures(
    val cause: String?,
    val percentage: String?
)

@Serializable
data class Price(
    val amount: Double?,
    val amountPayAtProperty: Int?,
    val amountPayableNow: Int?,
    val amountWithInclusiveTax: Double?,
    val amountWithTaxesFees: Double?,
    val amountWithFees:Double?,
    val currencyCode: String?,
    val type: String?,
    val tax: Tax?
)

@Serializable
data class NonIndianHotelsPrice(
    val amount: Int?,
    val amountPayAtProperty: Int?,
    val amountPayableNow: Int?,
    val amountWithInclusiveTax: Int?,
    val amountWithTaxesFees: Int?,
    val amountWithFees:Double?,
    val currencyCode: String?,
    val type: String?,
    val tax: Tax?
)

@Serializable
data class Tax(
    val amount:Double
)

@Serializable
data class PriceGroup(
    val amount: Double?,
    val amountPayAtProperty: Int?,
    val amountPayableNow: Int?,
    val amountWithInclusiveTax: Double?,
    val amountWithTaxesFees: Double?,
    val amountWithFees:Double?,
    val currencyCode: String?,
    val ref: String?,
    val refValue: String?,
    val type: String?
)

@Serializable
data class NonIndianHotelsPriceGroup(
    val amount: Int?,
    val amountPayAtProperty: Int?,
    val amountPayableNow: Int?,
    val amountWithInclusiveTax: Int?,
    val amountWithTaxesFees: Int?,
    val currencyCode: String?,
    val ref: String?,
    val refValue: String?,
    val type: String?
)
