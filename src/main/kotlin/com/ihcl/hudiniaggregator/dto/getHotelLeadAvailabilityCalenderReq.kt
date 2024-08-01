package com.ihcl.hudiniaggregator.dto

import kotlinx.serialization.Serializable


@Serializable
data class GetHotelLeadAvailabilityCalenderReq(
    val hotelId:String,
    val hotelIds:List<String>,
    val startDate:String,
    val endDate:String,
    val rateFilter:String?,
    val lengthOfStay:String?,
    val adults:String,
    val children:String,
    val numRooms:String,
    val rateCode:String?,
    val promoCode:String?,
    val promoType:String?,
    val agentId:String?,
    val agentType:String?
)
