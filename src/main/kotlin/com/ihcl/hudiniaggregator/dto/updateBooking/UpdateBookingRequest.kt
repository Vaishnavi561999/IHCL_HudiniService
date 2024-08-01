package com.ihcl.hudiniaggregator.dto.updateBooking

import com.ihcl.hudiniaggregator.dto.createBooking.GuestCounts

data class UpdateBookingRequest(
    val crsConfirmationNumber: String,
    val guests: List<Guest>,
    val hotelId: String,
    val notification: Notification,
    val roomStay: RoomStay,
    val status: String
)
data class RoomStay(
        val endDate: String,
        val guestCount: List<GuestCounts>?,
        val numRooms: Int,
        val products: List<RoomStayProduct>,
        val startDate: String
)
data class Product(
        val rateCode: String,
        val roomCode: String
)
data class RoomStayProduct(
        val endDate: String,
        val product: Product,
        val startDate: String
)
data class PersonName(
        val firstName: String,
        val lastName: String,
        val prefix: String
)
data class PaymentCard(
        val cardCode: String,
        val cardHolder: String,
        val cardNumber: String,
        val cardSecurityCode: String,
        val expireDate: String
)
data class Payment(
        val paymentCard: PaymentCard,
        val type: String
)
data class Notification(
        val bookingComment: String,
        val deliveryComments: DeliveryComments
)
data class Guest(
        val contactNumbers: List<ContactNumber>,
        val emailAddress: List<EmailAddress>,
        val payments: List<Payment>,
        val personName: PersonName
)
data class EmailAddress(
        val value: String
)
data class DeliveryComments(
        val comment: String
)
data class ContactNumber(
        val number: String
)