package com.ihcl.hudiniaggregator.dto.createBooking

data class CreateBookingResponse(
    val data: Data?
)

data class Data(
    val createHotelBooking: CreateHotelBooking?
)

data class CreateHotelBooking(
    val message: String?,
    val reservations: List<Reservation?>?
)

data class Reservation(
    val crsConfirmationNumber: String?,
    val itineraryNumber: String?
)