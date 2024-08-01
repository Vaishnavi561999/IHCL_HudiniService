package com.ihcl.hudiniaggregator.dto.createBooking

import kotlinx.serialization.Serializable

@Serializable
data class CreateBookingRequest(
    val currency: Currency?,
    val guests: List<Guest>?,
    val hotelId: String?,
    val notification: Notification?,
    val roomStay: RoomStays?,
    val status: String?,
    val itineraryNumber: String?,
    val promotionType:String?,
    val promotionAccessKey:String?,
    val travelIndustryId:String?,
    val couponOffersCode:String?,
    val loyaltyMemberships:LoyaltyMemberships?,
)
@Serializable
data class Currency(
    val code: String?
)

@Serializable
data class Guest(
    val emailAddress: List<EmailAddress?>?,
    val role: String?,
    val contactNumbers:List<ContactNumbers?>?
)
@Serializable
data class ContactNumbers(
    val number:String?,
    val code:String?,
    val role:String?,
    val sortOrder:Int?,
    val type:String?,
    val use:String?
)
@Serializable
data class EmailAddress(
    val type: String?,
    val value: String?
)

@Serializable
data class Notification(
    val bookingComment: String?,
    val deliveryComments: DeliveryComments?
)

@Serializable
data class DeliveryComments(
    val comment: String?
)

@Serializable
data class RoomStays(
    val endDate: String?,
    val guestCount: List<GuestCounts>?,
    val numRooms: Int?,
    val products: List<Products>?,
    val startDate: String?
)

@Serializable
data class GuestCounts(
    val ageQualifyingCode: String?,
    val numGuests: Int?
)

@Serializable
data class Products(
    val endDate: String?,
    val product: CAProduct?,
    val startDate: String?
)

@Serializable
data class CAProduct(
    val rateCode: String?,
    val roomCode: String?
)

@Serializable
data class GraphQueryToJSON(
    val query: String
)
@Serializable
data class LoyaltyMemberships(
    val levelCode:String?,
    val membershipID:String?,
    val programID:String?,
    val source:String?
)