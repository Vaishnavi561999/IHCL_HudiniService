package com.ihcl.hudiniaggregator.dto.getBookingDetails

data class BookingDetailsRequest(
    val confirmationNumber: String?,
    val hotelId: String?,
    val emailId:String?,
    val guestPhoneNumber:String?,
    val itineraryNumber:String?,
    val arrivalDate:String?
)