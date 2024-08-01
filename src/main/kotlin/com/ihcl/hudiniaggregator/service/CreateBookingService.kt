package com.ihcl.hudiniaggregator.service

import com.ihcl.hudiniaggregator.config.PropertiesConfiguration
import com.ihcl.hudiniaggregator.dto.createBooking.CreateBookingRequest
import com.ihcl.hudiniaggregator.plugins.ConfigureHTTPClient
import com.ihcl.hudiniaggregator.util.Constants
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.ihcl.hudiniaggregator.dto.createBooking.GraphQueryToJSON
import com.ihcl.hudiniaggregator.exceptions.InternalServerException
import com.ihcl.hudiniaggregator.util.Constants.CHAMBERS_GLOBAL_MEMBER
import com.ihcl.hudiniaggregator.util.Constants.CHAMBERS_NATIONAL_MEMBER
import com.ihcl.hudiniaggregator.util.Constants.CHG
import com.ihcl.hudiniaggregator.util.Constants.CHN
import com.ihcl.hudiniaggregator.util.Constants.EC
import com.ihcl.hudiniaggregator.util.Constants.EP
import com.ihcl.hudiniaggregator.util.Constants.EPICURE_CORPORATE
import com.ihcl.hudiniaggregator.util.Constants.EPICURE_PREFERRED
import com.ihcl.hudiniaggregator.util.Constants.EPICURE_PRIVILEGED
import com.ihcl.hudiniaggregator.util.Constants.EV
import com.ihcl.hudiniaggregator.util.Constants.TATA_DIGITAL_COPPER
import com.ihcl.hudiniaggregator.util.Constants.TATA_DIGITAL_GOLD
import com.ihcl.hudiniaggregator.util.Constants.TATA_DIGITAL_PLATINUM
import com.ihcl.hudiniaggregator.util.Constants.TATA_DIGITAL_SILVER
import com.ihcl.hudiniaggregator.util.Constants.TCPC
import com.ihcl.hudiniaggregator.util.Constants.TCPG
import com.ihcl.hudiniaggregator.util.Constants.TCPP
import com.ihcl.hudiniaggregator.util.Constants.TCPS
import com.ihcl.hudiniaggregator.util.GenerateToken
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent
import io.ktor.client.request.headers
import io.ktor.http.*


class CreateBookingService {
    private val prop = PropertiesConfiguration.env
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val generateToken by KoinJavaComponent.inject<GenerateToken>(GenerateToken::class.java)

    /* This method is used to call hudini create booking API*/
    suspend fun createBooking(data: CreateBookingRequest): HttpResponse {
        val token = generateToken.getToken()
        log.debug("Request received to create booking is {}.json", data)
        val mutation = prepareCreateBookingMutation(data)
        log.debug("_____GraphQl Mutation prepared for calling hudini create booking is_____")
        val jsonQuery = this.convertGraphQueryToJSONBody(mutation)
        log.debug("converted query to json $jsonQuery")
        try {
            val response: HttpResponse =
                ConfigureHTTPClient.client.post(prop.bookingDevUrl) {
                    timeout {
                        requestTimeoutMillis = prop.requestTimeoutMillis.toLong()
                    }
                    headers {
                        append(Constants.AUTHORIZATION, token)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(jsonQuery)
                }
            if (response.status == HttpStatusCode.Forbidden) {
                generateToken.generateTokenAndSave()
                return createBooking(data)
            }
            log.debug("Response received from api for create booking is ${response.bodyAsText()}")
            return response
        } catch (e: Exception) {
            log.error("Exception occurred while calling api is ${e.message} due to ${e.stackTrace}")
            throw InternalServerException(e.message)
        }
    }

    fun convertGraphQueryToJSONBody(graphQuery: String): String {
        val request = GraphQueryToJSON(graphQuery)
        return Json.encodeToString(GraphQueryToJSON.serializer(), request)
    }

    private fun prepareCreateBookingMutation(data: CreateBookingRequest): String {
        log.info("prepareCreateBookingMutation hotelId ${data.hotelId}")
        val mutation: String
        var loyaltyMembershipLevelName: String? = null
        if (!data.loyaltyMemberships?.membershipID.isNullOrEmpty() || !data.loyaltyMemberships?.programID.isNullOrEmpty()) {
            loyaltyMembershipLevelName = getLoyaltyMembershipLevelName(data)
        }
        mutation = when {
            (!data.promotionType.isNullOrEmpty() && !data.promotionAccessKey.isNullOrEmpty()) -> {
                constructPromoCodeMutation(data, loyaltyMembershipLevelName)
            }

            (!data.travelIndustryId.isNullOrEmpty()) -> {
                constructTravelAgentIdMutation(data, loyaltyMembershipLevelName)
            }

            (!data.couponOffersCode.isNullOrEmpty()) -> {
                constructCouponCodeMutation(data, loyaltyMembershipLevelName)
            }

            else -> {
                constructNoSpecialCodeMutation(data, loyaltyMembershipLevelName)
            }
        }
        return mutation
    }

    private fun constructNoSpecialCodeMutation(data: CreateBookingRequest, loyaltyMembershipLevelName: String?):String{
        val mutation: String
        val guestsBuilder = StringBuilder()
        for (guest in data.guests!!) {
            guestsBuilder.append("{")
            guestsBuilder.append("role: \"${guest.role}\", ")

            val emailAddressBuilder = StringBuilder()
            for (emailAddress in guest.emailAddress!!) {
                emailAddressBuilder.append("{")
                emailAddressBuilder.append("type: \"${emailAddress?.type}\", ")
                emailAddressBuilder.append("value: \"${emailAddress?.value}\"")
                emailAddressBuilder.append("}, ")
            }
            val emailAddressSection = emailAddressBuilder.toString().removeSuffix(", ")

            val contactNumbersBuilder = StringBuilder()
            for (contactNumbers in guest.contactNumbers!!) {
                contactNumbersBuilder.append("{")
                contactNumbersBuilder.append("number: \"${contactNumbers?.number}\", ")
                contactNumbersBuilder.append("code: \"${contactNumbers?.code}\", ")
                contactNumbersBuilder.append("role: \"${contactNumbers?.role}\", ")
                contactNumbersBuilder.append("sortOrder: ${contactNumbers?.sortOrder}")
                contactNumbersBuilder.append("type: \"${contactNumbers?.type}\", ")
                contactNumbersBuilder.append("use: \"${contactNumbers?.use}\", ")
                contactNumbersBuilder.append("}, ")
            }
            val contactNumbersSection = contactNumbersBuilder.toString().removeSuffix(", ")

            guestsBuilder.append("emailAddress: [$emailAddressSection]")
            guestsBuilder.append("contactNumbers: [${contactNumbersSection}]")
            guestsBuilder.append("}, ")
        }
        val guestsSection = guestsBuilder.toString().removeSuffix(", ")

        val guestCountBuilder = StringBuilder()
        for (guestCount in data.roomStay?.guestCount!!) {
            guestCountBuilder.append("{")
            guestCountBuilder.append("ageQualifyingCode: \"${guestCount.ageQualifyingCode}\", ")
            guestCountBuilder.append("numGuests: ${guestCount.numGuests}")
            guestCountBuilder.append("}, ")
        }
        val guestCountSection = guestCountBuilder.toString().removeSuffix(", ")

        val productsBuilder = StringBuilder()
        for (product in data.roomStay.products!!) {
            productsBuilder.append("{")
            productsBuilder.append("product: { rateCode: \"${product.product?.rateCode}\", ")
            productsBuilder.append("roomCode: \"${product.product?.roomCode}\"")
            productsBuilder.append("}, ")
            productsBuilder.append("startDate: \"${product.startDate}\", ")
            productsBuilder.append("endDate: \"${product.endDate}\"")
            productsBuilder.append("}, ")
        }
        val productsSection = productsBuilder.toString().removeSuffix(", ")
        if (data.loyaltyMemberships?.membershipID.isNullOrEmpty()) {
            mutation = """
            mutation {
                createHotelBooking(createBooking: {
                    hotelId: "${data.hotelId}",
                    status: "${data.status}",
                    itineraryNumber: "${data.itineraryNumber}"
                    currency: {code: "${data.currency?.code}"},
                    guests: [$guestsSection],
                    roomStay: {
                        startDate: "${data.roomStay.startDate}", 
                        endDate: "${data.roomStay.endDate}",
                        guestCount: [$guestCountSection],
                        numRooms: ${data.roomStay.numRooms},
                        products: [$productsSection]
                    },
                    notification: {
                        bookingComment: "${data.notification?.bookingComment}",
                        deliveryComments: {   
                            comment: "${data.notification?.deliveryComments?.comment}"
                        }
                    }
                }) {
                    reservations {
                        crsConfirmationNumber
                        itineraryNumber
                    }
                    message
                }
            }
        """.trimIndent()
        } else {
            mutation = """
            mutation {
                createHotelBooking(createBooking: {
                    hotelId: "${data.hotelId}",
                    status: "${data.status}",
                    itineraryNumber: "${data.itineraryNumber}"
                    currency: {code: "${data.currency?.code}"},
                    guests: [$guestsSection],
                    roomStay: {
                        startDate: "${data.roomStay.startDate}", 
                        endDate: "${data.roomStay.endDate}",
                        guestCount: [$guestCountSection],
                        numRooms: ${data.roomStay.numRooms},
                        products: [$productsSection]
                    },
                    notification: {
                        bookingComment: "${data.notification?.bookingComment}",
                        deliveryComments: {   
                            comment: "${data.notification?.deliveryComments?.comment}"
                        }
                    }
                    loyaltyMemberships:{
                        level:{
                            code:"${data.loyaltyMemberships?.levelCode}",
                            name:"$loyaltyMembershipLevelName"
                        },
                        membershipID:"${data.loyaltyMemberships?.membershipID}",
                        programID:"${data.loyaltyMemberships?.programID}",
                        source:"${data.loyaltyMemberships?.source}"
                    }
                }) {
                    reservations {
                        crsConfirmationNumber
                        itineraryNumber
                    }
                    message
                }
            }
        """.trimIndent()
        }
        log.info("create booking no special codes query")
        return mutation
    }
    private fun constructPromoCodeMutation(data: CreateBookingRequest, loyaltyMembershipLevelName: String?): String {
        val mutation: String
        val guestsBuilder = StringBuilder()
        for (guest in data.guests!!) {
            guestsBuilder.append("{")
            guestsBuilder.append("role: \"${guest.role}\", ")

            val emailAddressBuilder = StringBuilder()
            for (emailAddress in guest.emailAddress!!) {
                emailAddressBuilder.append("{")
                emailAddressBuilder.append("type: \"${emailAddress?.type}\", ")
                emailAddressBuilder.append("value: \"${emailAddress?.value}\"")
                emailAddressBuilder.append("}, ")
            }
            val emailAddressSection = emailAddressBuilder.toString().removeSuffix(", ")

            val contactNumbersBuilder = StringBuilder()
            for (contactNumbers in guest.contactNumbers!!) {
                contactNumbersBuilder.append("{")
                contactNumbersBuilder.append("number: \"${contactNumbers?.number}\", ")
                contactNumbersBuilder.append("code: \"${contactNumbers?.code}\", ")
                contactNumbersBuilder.append("role: \"${contactNumbers?.role}\", ")
                contactNumbersBuilder.append("sortOrder: ${contactNumbers?.sortOrder}")
                contactNumbersBuilder.append("type: \"${contactNumbers?.type}\", ")
                contactNumbersBuilder.append("use: \"${contactNumbers?.use}\", ")
                contactNumbersBuilder.append("}, ")
            }
            val contactNumbersSection = contactNumbersBuilder.toString().removeSuffix(", ")

            guestsBuilder.append("emailAddress: [$emailAddressSection]")
            guestsBuilder.append("contactNumbers: [${contactNumbersSection}]")
            guestsBuilder.append("}, ")
        }
        val guestsSection = guestsBuilder.toString().removeSuffix(", ")

        val guestCountBuilder = StringBuilder()
        for (guestCount in data.roomStay?.guestCount!!) {
            guestCountBuilder.append("{")
            guestCountBuilder.append("ageQualifyingCode: \"${guestCount.ageQualifyingCode}\", ")
            guestCountBuilder.append("numGuests: ${guestCount.numGuests}")
            guestCountBuilder.append("}, ")
        }
        val guestCountSection = guestCountBuilder.toString().removeSuffix(", ")

        val productsBuilder = StringBuilder()
        for (product in data.roomStay.products!!) {
            productsBuilder.append("{")
            productsBuilder.append("product: { rateCode: \"${product.product?.rateCode}\", ")
            productsBuilder.append("roomCode: \"${product.product?.roomCode}\"")
            productsBuilder.append("}, ")
            productsBuilder.append("startDate: \"${product.startDate}\", ")
            productsBuilder.append("endDate: \"${product.endDate}\"")
            productsBuilder.append("}, ")
        }
        val productsSection = productsBuilder.toString().removeSuffix(", ")
        if (data.loyaltyMemberships?.membershipID.isNullOrEmpty()) {
            mutation = """
            mutation {
                createHotelBooking(createBooking: {
                    hotelId: "${data.hotelId}",
                    status: "${data.status}",
                    itineraryNumber: "${data.itineraryNumber}"
                    currency: {code: "${data.currency?.code}"},
                    promotion: {
                       type: "${data.promotionType}"
                       accessKey: {password: "${data.promotionAccessKey}"}
                    },                   
                    guests: [$guestsSection],
                    roomStay: {
                        startDate: "${data.roomStay.startDate}", 
                        endDate: "${data.roomStay.endDate}",
                        guestCount: [$guestCountSection],
                        numRooms: ${data.roomStay.numRooms},
                        products: [$productsSection]
                    },
                    notification: {
                        bookingComment: "${data.notification?.bookingComment}",
                        deliveryComments: {   
                            comment: "${data.notification?.deliveryComments?.comment}"
                        }
                    }
                }) {
                    reservations {
                        crsConfirmationNumber
                        itineraryNumber
                    }
                    message
                }
            }
        """.trimIndent()
        } else {
            mutation = """
            mutation {
                createHotelBooking(createBooking: {
                    hotelId: "${data.hotelId}",
                    status: "${data.status}",
                    itineraryNumber: "${data.itineraryNumber}"
                    currency: {code: "${data.currency?.code}"},
                    promotion: {
                       type: "${data.promotionType}"
                       accessKey: {password: "${data.promotionAccessKey}"}
                    },                   
                    guests: [$guestsSection],
                    roomStay: {
                        startDate: "${data.roomStay.startDate}", 
                        endDate: "${data.roomStay.endDate}",
                        guestCount: [$guestCountSection],
                        numRooms: ${data.roomStay.numRooms},
                        products: [$productsSection]
                    },
                    notification: {
                        bookingComment: "${data.notification?.bookingComment}",
                        deliveryComments: {   
                            comment: "${data.notification?.deliveryComments?.comment}"
                        }
                    }
                    loyaltyMemberships:{
                        level:{
                            code:"${data.loyaltyMemberships?.levelCode}",
                            name:"$loyaltyMembershipLevelName"
                        },
                        membershipID:"${data.loyaltyMemberships?.membershipID}",
                        programID:"${data.loyaltyMemberships?.programID}",
                        source:"${data.loyaltyMemberships?.source}"
                    }
                }) {
                    reservations {
                        crsConfirmationNumber
                        itineraryNumber
                    }
                    message
                }
            }
        """.trimIndent()
        }
        log.info("create booking promo code query")
        return mutation
    }

    private fun constructTravelAgentIdMutation(
        data: CreateBookingRequest,
        loyaltyMembershipLevelName: String?,
    ): String {
        val mutation: String
        val guestsBuilder = StringBuilder()
        for (guest in data.guests!!) {
            guestsBuilder.append("{")
            guestsBuilder.append("role: \"${guest.role}\", ")

            val emailAddressBuilder = StringBuilder()
            for (emailAddress in guest.emailAddress!!) {
                emailAddressBuilder.append("{")
                emailAddressBuilder.append("type: \"${emailAddress?.type}\", ")
                emailAddressBuilder.append("value: \"${emailAddress?.value}\"")
                emailAddressBuilder.append("}, ")
            }
            val emailAddressSection = emailAddressBuilder.toString().removeSuffix(", ")

            val contactNumbersBuilder = StringBuilder()
            for (contactNumbers in guest.contactNumbers!!) {
                contactNumbersBuilder.append("{")
                contactNumbersBuilder.append("number: \"${contactNumbers?.number}\", ")
                contactNumbersBuilder.append("code: \"${contactNumbers?.code}\", ")
                contactNumbersBuilder.append("role: \"${contactNumbers?.role}\", ")
                contactNumbersBuilder.append("sortOrder: ${contactNumbers?.sortOrder}")
                contactNumbersBuilder.append("type: \"${contactNumbers?.type}\", ")
                contactNumbersBuilder.append("use: \"${contactNumbers?.use}\", ")
                contactNumbersBuilder.append("}, ")
            }
            val contactNumbersSection = contactNumbersBuilder.toString().removeSuffix(", ")

            guestsBuilder.append("emailAddress: [$emailAddressSection]")
            guestsBuilder.append("contactNumbers: [${contactNumbersSection}]")
            guestsBuilder.append("}, ")
        }
        val guestsSection = guestsBuilder.toString().removeSuffix(", ")

        val guestCountBuilder = StringBuilder()
        for (guestCount in data.roomStay?.guestCount!!) {
            guestCountBuilder.append("{")
            guestCountBuilder.append("ageQualifyingCode: \"${guestCount.ageQualifyingCode}\", ")
            guestCountBuilder.append("numGuests: ${guestCount.numGuests}")
            guestCountBuilder.append("}, ")
        }
        val guestCountSection = guestCountBuilder.toString().removeSuffix(", ")

        val productsBuilder = StringBuilder()
        for (product in data.roomStay.products!!) {
            productsBuilder.append("{")
            productsBuilder.append("product: { rateCode: \"${product.product?.rateCode}\", ")
            productsBuilder.append("roomCode: \"${product.product?.roomCode}\"")
            productsBuilder.append("}, ")
            productsBuilder.append("startDate: \"${product.startDate}\", ")
            productsBuilder.append("endDate: \"${product.endDate}\"")
            productsBuilder.append("}, ")
        }
        val productsSection = productsBuilder.toString().removeSuffix(", ")
        if (data.loyaltyMemberships?.membershipID.isNullOrEmpty()) {
            mutation = """
            mutation {
                createHotelBooking(createBooking: {
                    hotelId: "${data.hotelId}",
                    status: "${data.status}",
                    itineraryNumber: "${data.itineraryNumber}",
                    currency: {code: "${data.currency?.code}"},
                    commissionableAccountProfile: {
                        travelIndustryId: "${data.travelIndustryId}"
                    },
                    guests: [$guestsSection],
                    roomStay: {
                        startDate: "${data.roomStay.startDate}", 
                        endDate: "${data.roomStay.endDate}",
                        guestCount: [$guestCountSection],
                        numRooms: ${data.roomStay.numRooms},
                        products: [$productsSection]
                    },
                    notification: {
                        bookingComment: "${data.notification?.bookingComment}",
                        deliveryComments: {   
                            comment: "${data.notification?.deliveryComments?.comment}"
                        }
                    }
                }) {
                    reservations {
                        crsConfirmationNumber
                        itineraryNumber
                    }
                    message
                }
            }
        """.trimIndent()
        } else {
            mutation = """
            mutation {
                createHotelBooking(createBooking: {
                    hotelId: "${data.hotelId}",
                    status: "${data.status}",
                    itineraryNumber: "${data.itineraryNumber}",
                    currency: {code: "${data.currency?.code}"},
                    commissionableAccountProfile: {
                        travelIndustryId: "${data.travelIndustryId}"
                    },
                    guests: [$guestsSection],
                    roomStay: {
                        startDate: "${data.roomStay.startDate}", 
                        endDate: "${data.roomStay.endDate}",
                        guestCount: [$guestCountSection],
                        numRooms: ${data.roomStay.numRooms},
                        products: [$productsSection]
                    },
                    notification: {
                        bookingComment: "${data.notification?.bookingComment}",
                        deliveryComments: {   
                            comment: "${data.notification?.deliveryComments?.comment}"
                        }
                    }
                    loyaltyMemberships:{
                        level:{
                            code:"${data.loyaltyMemberships?.levelCode}",
                            name:"$loyaltyMembershipLevelName"
                        },
                        membershipID:"${data.loyaltyMemberships?.membershipID}",
                        programID:"${data.loyaltyMemberships?.programID}",
                        source:"${data.loyaltyMemberships?.source}"
                    }
                }) {
                    reservations {
                        crsConfirmationNumber
                        itineraryNumber
                    }
                    message
                }
            }
        """.trimIndent()
        }
        log.info("create booking agent Id query")
        return mutation
    }

    private fun constructCouponCodeMutation(data: CreateBookingRequest, loyaltyMembershipLevelName: String?): String {
        val mutation: String
        val guestsBuilder = StringBuilder()
        for (guest in data.guests!!) {
            guestsBuilder.append("{")
            guestsBuilder.append("role: \"${guest.role}\", ")

            val emailAddressBuilder = StringBuilder()
            for (emailAddress in guest.emailAddress!!) {
                emailAddressBuilder.append("{")
                emailAddressBuilder.append("type: \"${emailAddress?.type}\", ")
                emailAddressBuilder.append("value: \"${emailAddress?.value}\"")
                emailAddressBuilder.append("}, ")
            }
            val emailAddressSection = emailAddressBuilder.toString().removeSuffix(", ")

            val contactNumbersBuilder = StringBuilder()
            for (contactNumbers in guest.contactNumbers!!) {
                contactNumbersBuilder.append("{")
                contactNumbersBuilder.append("number: \"${contactNumbers?.number}\", ")
                contactNumbersBuilder.append("code: \"${contactNumbers?.code}\", ")
                contactNumbersBuilder.append("role: \"${contactNumbers?.role}\", ")
                contactNumbersBuilder.append("sortOrder: ${contactNumbers?.sortOrder}")
                contactNumbersBuilder.append("type: \"${contactNumbers?.type}\", ")
                contactNumbersBuilder.append("use: \"${contactNumbers?.use}\", ")
                contactNumbersBuilder.append("}, ")
            }
            val contactNumbersSection = contactNumbersBuilder.toString().removeSuffix(", ")

            guestsBuilder.append("emailAddress: [$emailAddressSection]")
            guestsBuilder.append("contactNumbers: [${contactNumbersSection}]")
            guestsBuilder.append("}, ")
        }
        val guestsSection = guestsBuilder.toString().removeSuffix(", ")

        val guestCountBuilder = StringBuilder()
        for (guestCount in data.roomStay?.guestCount!!) {
            guestCountBuilder.append("{")
            guestCountBuilder.append("ageQualifyingCode: \"${guestCount.ageQualifyingCode}\", ")
            guestCountBuilder.append("numGuests: ${guestCount.numGuests}")
            guestCountBuilder.append("}, ")
        }
        val guestCountSection = guestCountBuilder.toString().removeSuffix(", ")

        val productsBuilder = StringBuilder()
        for (product in data.roomStay.products!!) {
            productsBuilder.append("{")
            productsBuilder.append("product: { rateCode: \"${product.product?.rateCode}\", ")
            productsBuilder.append("roomCode: \"${product.product?.roomCode}\"")
            productsBuilder.append("}, ")
            productsBuilder.append("startDate: \"${product.startDate}\", ")
            productsBuilder.append("endDate: \"${product.endDate}\"")
            productsBuilder.append("}, ")
        }
        val productsSection = productsBuilder.toString().removeSuffix(", ")
        if (data.loyaltyMemberships?.membershipID.isNullOrEmpty()) {
            mutation = """
            mutation {
                createHotelBooking(createBooking: {
                    hotelId: "${data.hotelId}",
                    status: "${data.status}",
                    itineraryNumber: "${data.itineraryNumber}",
                    currency: {code: "${data.currency?.code}"},
                    couponOffers: [{code: "${data.couponOffersCode}" }],
                    guests: [$guestsSection],
                    roomStay: {
                        startDate: "${data.roomStay.startDate}", 
                        endDate: "${data.roomStay.endDate}",
                        guestCount: [$guestCountSection],
                        numRooms: ${data.roomStay.numRooms},
                        products: [$productsSection]
                    },
                    notification: {
                        bookingComment: "${data.notification?.bookingComment}",
                        deliveryComments: {   
                            comment: "${data.notification?.deliveryComments?.comment}"
                        }
                    }
                }) {
                    reservations {
                        crsConfirmationNumber
                        itineraryNumber
                    }
                    message
                }
            }
        """.trimIndent()
        } else {
            mutation = """
            mutation {
                createHotelBooking(createBooking: {
                    hotelId: "${data.hotelId}",
                    status: "${data.status}",
                    itineraryNumber: "${data.itineraryNumber}",
                    currency: {code: "${data.currency?.code}"},
                    couponOffers: [{code: "${data.couponOffersCode}" }],
                    guests: [$guestsSection],
                    roomStay: {
                        startDate: "${data.roomStay.startDate}", 
                        endDate: "${data.roomStay.endDate}",
                        guestCount: [$guestCountSection],
                        numRooms: ${data.roomStay.numRooms},
                        products: [$productsSection]
                    },
                    notification: {
                        bookingComment: "${data.notification?.bookingComment}",
                        deliveryComments: {   
                            comment: "${data.notification?.deliveryComments?.comment}"
                        }
                    }
                    loyaltyMemberships:{
                        level:{
                            code:"${data.loyaltyMemberships?.levelCode}",
                            name:"$loyaltyMembershipLevelName"
                        },
                        membershipID:"${data.loyaltyMemberships?.membershipID}",
                        programID:"${data.loyaltyMemberships?.programID}",
                        source:"${data.loyaltyMemberships?.source}"
                    }
                }) {
                    reservations {
                        crsConfirmationNumber
                        itineraryNumber
                    }
                    message
                }
            }
        """.trimIndent()
        }
        log.info("create booking coupon code query")
        return mutation
    }

    private fun getLoyaltyMembershipLevelName(request: CreateBookingRequest): String? {
        var loyaltyMembershipLevelName: String? = null
        when (request.loyaltyMemberships?.levelCode) {
            TCPG -> {
                loyaltyMembershipLevelName = TATA_DIGITAL_GOLD
            }

            TCPS -> {
                loyaltyMembershipLevelName = TATA_DIGITAL_SILVER
            }

            TCPP -> {
                loyaltyMembershipLevelName = TATA_DIGITAL_PLATINUM
            }

            TCPC -> {
                loyaltyMembershipLevelName = TATA_DIGITAL_COPPER
            }

            CHN -> {
                loyaltyMembershipLevelName = CHAMBERS_NATIONAL_MEMBER
            }

            CHG -> {
                loyaltyMembershipLevelName = CHAMBERS_GLOBAL_MEMBER
            }

            EC -> {
                loyaltyMembershipLevelName = EPICURE_CORPORATE
            }

            EP -> {
                loyaltyMembershipLevelName = EPICURE_PREFERRED
            }

            EV -> {
                loyaltyMembershipLevelName = EPICURE_PRIVILEGED
            }
        }
        log.info("loyalty membership name: $loyaltyMembershipLevelName")
        return loyaltyMembershipLevelName
    }
}