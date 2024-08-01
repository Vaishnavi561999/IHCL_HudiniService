package com.ihcl.hudiniaggregator.dto.checkAvailability

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class CheckAvailabilityInput (
    val hotelId:String?,
    val startDate: String?,
    val endDate: String?,
    val numRooms: Int?,
    val adults: Int?,
    val children:String?,
    val rateFilter:String?,
    val memberTier:String?,
    val rateCode:String?,
    val promoCode:String?,
    val promoType:String?,
    @SerializedName("package")
    val packageFilter:String?,
    val agentType:String?,
    val agentId:String?,
    val couponCode:String?,
    val isOfferLandingPage:Boolean?,
    val isEmployeeOffer:Boolean?,
    val isMyAccount:Boolean?,
    val isCorporate:Boolean?,
    val isLogin:Boolean?,
    val isMemberOffer1:Boolean?,
    val isMemberOffer2:Boolean?
)