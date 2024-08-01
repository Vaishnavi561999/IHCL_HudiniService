package com.ihcl.hudiniaggregator.dto.updateBooking

data class UpdateBookingResponse(
    val data: Data?
)
data class Data(
    val updateHotelBooking: UpdateHotelBooking?
)
data class UpdateHotelBooking(
    val message: String?,
    val reservations: List<Reservation?>?
)
data class Reservation(
    val crsConfirmationNumber: String?,
    val itineraryNumber: String?
)