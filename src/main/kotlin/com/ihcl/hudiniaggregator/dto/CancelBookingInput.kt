package com.ihcl.hudiniaggregator.dto

data class CancelBookingInput(
    val crsConfirmationNumber: String?,
    val cancellationReason: String?,
    val hotelId: String?
)