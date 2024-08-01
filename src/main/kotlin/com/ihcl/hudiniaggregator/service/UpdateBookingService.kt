package com.ihcl.hudiniaggregator.service

import com.ihcl.hudiniaggregator.config.PropertiesConfiguration
import com.ihcl.hudiniaggregator.dto.updateBooking.UpdateBookingRequest
import com.ihcl.hudiniaggregator.exceptions.InternalServerException
import com.ihcl.hudiniaggregator.plugins.ConfigureHTTPClient
import com.ihcl.hudiniaggregator.util.Constants
import com.ihcl.hudiniaggregator.util.GenerateToken
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import org.koin.java.KoinJavaComponent
import org.litote.kmongo.json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.ktor.client.request.headers

class UpdateBookingService {
    private val createBookingService by KoinJavaComponent.inject<CreateBookingService>(CreateBookingService::class.java)
    private val generateToken by KoinJavaComponent.inject<GenerateToken>(GenerateToken::class.java)
    private val prop = PropertiesConfiguration.env
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /* This method is used to call hudini update booking API */
    suspend fun updateBookingDetails(data:UpdateBookingRequest):HttpResponse{
        val token = generateToken.getToken()
        log.debug("Request received to update booking details to hudini is ${data.json}")
        val guestsBuilder = StringBuilder()
        for(guest in data.guests){
            guestsBuilder.append("{")

            guestsBuilder.append("personName: {")
            guestsBuilder.append("firstName: \"${guest.personName.firstName}\", ")
            guestsBuilder.append("prefix: \"${guest.personName.prefix}\", ")
            guestsBuilder.append("lastName: \"${guest.personName.lastName}\", ")
            guestsBuilder.append("}, ")

            val emailAddressBuilder = StringBuilder()
            for(emailAddresses in guest.emailAddress) {
                emailAddressBuilder.append("{")
                emailAddressBuilder.append("value: \"${emailAddresses.value}\", ")
                emailAddressBuilder.append("},")
            }
            val emailAddressSection = emailAddressBuilder.toString().removeSuffix(", ")

            guestsBuilder.append("emailAddress: [$emailAddressSection], ")

            val contactNumberBuilder = StringBuilder()
            for(contactNumbers in guest.contactNumbers){
                contactNumberBuilder.append("{")
                contactNumberBuilder.append("number: \"${contactNumbers.number}\", ")
                contactNumberBuilder.append("type: \"${Constants.MOBILE}\", ")
                contactNumberBuilder.append("},")
            }
            val contactNumberSection = contactNumberBuilder.toString().removeSuffix(", ")

            guestsBuilder.append("contactNumbers: [${contactNumberSection}], ")

            val paymentsBuilder = StringBuilder()
            for(paymentS in guest.payments){
                paymentsBuilder.append("{")
                paymentsBuilder.append("paymentCard: {")
                paymentsBuilder.append("cardCode: \"${data.guests[0].payments[0].paymentCard.cardCode}\", ")
                paymentsBuilder.append("cardHolder: \"${data.guests[0].payments[0].paymentCard.cardHolder}\", ")
                paymentsBuilder.append("cardNumber: \"${data.guests[0].payments[0].paymentCard.cardNumber}\", ")
                paymentsBuilder.append("cardSecurityCode: \"${data.guests[0].payments[0].paymentCard.cardSecurityCode}\", ")
                paymentsBuilder.append("expireDate: \"${data.guests[0].payments[0].paymentCard.expireDate}\", ")
                paymentsBuilder.append("}, ")
                paymentsBuilder.append("type: \"${data.guests[0].payments[0].type}\", ")
                paymentsBuilder.append("}, ")
            }
            val paymentsSection = paymentsBuilder.toString().removeSuffix(", ")

            guestsBuilder.append("payments: [${paymentsSection}], ")
            guestsBuilder.append("}, ")
        }
        val guestsSection = guestsBuilder.toString().removeSuffix(", ")

        val guestCountBuilder = StringBuilder()
        for (guestCount in data.roomStay.guestCount!!) {
            guestCountBuilder.append("{")
            guestCountBuilder.append("ageQualifyingCode: \"${guestCount.ageQualifyingCode}\", ")
            guestCountBuilder.append("numGuests: ${guestCount.numGuests}")
            guestCountBuilder.append("}, ")
        }
        val guestCountSection = guestCountBuilder.toString().removeSuffix(", ")

        val productsBuilder = StringBuilder()
        for (products in data.roomStay.products){
            productsBuilder.append("{")
            productsBuilder.append("startDate: \"${products.startDate}\", ")
            productsBuilder.append("endDate: \"${products.endDate}\", ")
            productsBuilder.append("product: {")
            productsBuilder.append("rateCode: \"${products.product.rateCode}\", ")
            productsBuilder.append("roomCode: \"${products.product.roomCode}\", ")
            productsBuilder.append("}, ")
            productsBuilder.append("}, ")
        }
        val productsSection = productsBuilder.toString().removeSuffix(", ")

        val mutation = """
             mutation {updateHotelBooking(updateBooking:{
                hotelId: "${data.hotelId}",
                crsConfirmationNumber: "${data.crsConfirmationNumber}",
                status: "${data.status}",
              	 guests: [${guestsSection}],
                roomStay: {
              	  guestCount: [$guestCountSection]
                  numRooms: ${data.roomStay.numRooms},
                  products: [${productsSection}],
                  startDate: "${data.roomStay.startDate}", 
                  endDate: "${data.roomStay.endDate}"
                  },
                  notification: {bookingComment: "${data.notification.bookingComment}",
                  deliveryComments: {comment: "${data.notification.deliveryComments.comment}"}}
                  }
            )
              {
              reservations {
                crsConfirmationNumber
                itineraryNumber
              }
			  errorCode
              message
            } 
            }
        """.trimIndent()
        log.debug("_____GraphQl Mutation is prepared to call hudini update booking API is_____")
        val jsonQuery = createBookingService.convertGraphQueryToJSONBody(mutation)
        log.debug("converted query to json $jsonQuery")
        try{
            val response: HttpResponse =
                ConfigureHTTPClient.client.post(prop.bookingDevUrl) {
                    timeout {
                        requestTimeoutMillis = prop.requestTimeoutMillis.toLong()
                    }
                    headers{
                        append(Constants.AUTHORIZATION,token)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(jsonQuery)
                }
            if(response.status== HttpStatusCode.Forbidden){
                generateToken.generateTokenAndSave()
                return updateBookingDetails(data)
            }
            log.debug("Response received from hudini api for update booking is ${response.bodyAsText()}")
            return response
        }catch (e:Exception){
            log.error("Exception occurred while calling api is ${e.message} due to ${e.stackTrace}")
            throw InternalServerException(e.message)
        }

    }
}