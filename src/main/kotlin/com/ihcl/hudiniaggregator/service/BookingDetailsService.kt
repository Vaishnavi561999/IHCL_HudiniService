package com.ihcl.hudiniaggregator.service

import com.ihcl.hudiniaggregator.config.PropertiesConfiguration
import com.ihcl.hudiniaggregator.dto.getBookingDetails.BookingDetailsRequest
import com.ihcl.hudiniaggregator.exceptions.HttpResponseException
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


class BookingDetailsService {
    private val prop = PropertiesConfiguration.env
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val generateToken by KoinJavaComponent.inject<GenerateToken>(GenerateToken::class.java)
    private val createBooking by KoinJavaComponent.inject<CreateBookingService>(CreateBookingService::class.java)

    /* This method is used to call the hudini getBookingDetails API by email id and confirmation number based on request*/
    suspend fun getBookingDetails(request: BookingDetailsRequest): HttpResponse {
        log.info("Request received to get Booking Details $request")

        return when{
            (!request.confirmationNumber.isNullOrEmpty())->{
                if(request.confirmationNumber.isEmpty()){
                    throw HttpResponseException(Constants.REQUIRED_MANDATORY_FIELDS, HttpStatusCode.BadRequest)
                }
                log.info("Calling getBookingDetails API with confirmation id")
                val query = """
              query {
              getHotelBookingDetails(getBooking: {hotelId:"e84bb5c3-b460-4b25-a2ee-9bb0c5221cfb", confirmationNumber: "${request.confirmationNumber}",view: "${Constants.CONTENT}"}) 
              {
                    reservations {
                        modificationPermitted
                        cancellationPermitted
                        crsConfirmationNumber
                        itineraryNumber
                        externalReferenceNumber
                        id
                        channelConfirmationNumber
                        status
                        onHoldReleaseTime
                        onPropertyStatus
                        purposeOfStay
                        singleUsePaymentCardAllowed
                        sortOrder
                        roomStay{
                            numRooms
                            endDate
                            startDate
                            products{
                                startDate
                                endDate
                                primary
                                product{
                                    rateCode
                                    roomCode
                                    id
                                }
                            }
                            guestCount{
                                ageQualifyingCode
                                numGuests
                            }
                        }
                        hotel{
                            id
                            code
                            name
                        }
                        brand{
                            code
                            name
                        }
                        currency{
                            name
                            symbol
                            code
                        }
                        content{
                            roomCategories {
                                description
                                categoryCode
                                name    
                            }
                            rateCategories {
                                description
                                categoryCode
                                name   
                            }
                            rooms {
                                description
                                detailedDescription
                                categoryCode
                                code
                                name
                            }
                            rates {
                                description
                                detailedDescription
                                displayName
                                categoryCode
                                code
                                effectiveDate
                                expireDate
                                name
                                primary
                            }    
                        }
                        bookingDues {
                            noShowCharge {
                                amount
                            }
                            cancelPenalty{
                                amount
                                deadline
                                chargeList
                            }
                            deposit {
                                dueDate
                                amount
                                amountWithoutTax
                                status
                            }
                        }
                        notification {
                            sendGuestEmail
                            sendBookerEmail
                            bookingComment
                            deliveryComments {
                                comment
                            }
                        }
                        onPropertyInstructions {
                            chargeRoutingList
                        }
                        guests {
                            emailAddress {
                                type
                                value
                                default
                            }
                            contactNumbers {
                                number
                                code
                                default
                                sortOrder
                                type
                                role
                                use
                            }
                            marketingOptIn
                            role
                            startDate
                            endDate
                            payments {
                                role
                                type
                                vendorStatus
                                paymentCard {
                                    cardHolder
                                    cardCode
                                    cardName
                                    cardNumber
                                    token
                                    expireDate
                                }
                            }
                            personName {
                                prefix
                                firstName
                                lastName
                            }    
                        }
                        roomPrices {
                            averagePrice {
                                price {
                                    tax {
                                        amount
                                        breakDown {
                                            amount
                                            code
                                            name
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    fees {
                                        amount
                                         breakDown {
                                            amount
                                            code
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    displayOverrideAsPercentage
                                    originalAmount
                                    totalAmount
                                    amountPayableNow
                                    amountPayAtProperty
                                    totalAmountWithInclusiveTaxesFees
                                    totalAmountIncludingTaxesFees
                                    originalAmountIncludingTaxesAndFees
                                }
                            }
                            totalPrice {
                                price {
                                    tax {
                                        amount
                                        breakDown {
                                            amount
                                            code
                                            name
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    fees {
                                        amount
                                         breakDown {
                                            amount
                                            code
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    displayOverrideAsPercentage
                                    originalAmount
                                    totalAmount
                                    amountPayAtProperty
                                    totalAmountWithInclusiveTaxesFees
                                    totalAmountIncludingTaxesFees
                                    originalAmountIncludingTaxesAndFees
                                }
                            }
                            priceBreakdowns {
                                type
                                productPrices {
                                    startDate
                                    endDate
                                    product {
                                        rateCode
                                        roomCode
                                    }
                                    price {
                                        tax {
                                            amount
                                            breakDown {
                                                amount
                                                code
                                                name
                                                isPayAtProperty
                                                isInclusive
                                                isPerStay
                                            }
                                        }
                                        fees {
                                            amount
                                             breakDown {
                                                amount
                                                code
                                                isPayAtProperty
                                                isInclusive
                                                isPerStay
                                            }
                                        }
                                        displayOverrideAsPercentage
                                        originalAmount
                                        totalAmount
                                        amountPayableNow
                                        amountPayAtProperty
                                        totalAmountWithInclusiveTaxesFees
                                        totalAmountIncludingTaxesFees
                                        originalAmountIncludingTaxesAndFees
                                    }
                                }
                            }
                        }
                        overrides
                        discounts {
                            adjustmentAmount
                            adjustmentPercentage
                            type
                        }
                        cancelPolicy {
                            cancelFeeAmount {
                                taxInclusive
                                value
                            }
                            cancelFeeType
                            cancelPenaltyDate
                            cancelTime
                            cancelTimeIn
                            chargeType
                            chargeThreshold
                            description
                            modificationRestrictions
                            noShowFeeType
                            code
                            lateCancellationPermitted
                            charges {
                                daysToArrive
                                cancelFeeIncludesTax
                                cancelFeeAmount
                            }
                            noShowFeeAmount {
                                taxInclusive
                                value
                            }
                        }
                        bookingPolicy {
                            description
                            transactionFeeDisclaimer
                            guaranteeLevel
                            holdTime
                            allowPay
                            code
                            requirements
                            depositFee {
                                amount
                                dueDays
                                dueType
                                taxInclusive
                                isPrePayment
                                type
                            }
                        }
                        channels {
                            primaryChannel {
                                code
                            }
                        }
                    }
                    errorCode
                    message
                }
            }
            """.trimIndent()
                log.debug("_____Graphql query prepared for calling getBookingDetails API by confirmation id_____")
                val jsonBody = createBooking.convertGraphQueryToJSONBody(query)
                log.debug("json query prepared as ${jsonBody.json}")
                callGetBookingDetails(jsonBody)
            }
            (!request.guestPhoneNumber.isNullOrEmpty())->{
                if(request.guestPhoneNumber.isEmpty()){
                    throw HttpResponseException(Constants.REQUIRED_MANDATORY_FIELDS, HttpStatusCode.BadRequest)
                }
                log.info("Calling getBookingDetails API with mobile number")
                val query = """
              query {
              getHotelBookingDetails(getBooking: {hotelId:"e84bb5c3-b460-4b25-a2ee-9bb0c5221cfb", guestPhoneNumber: "${request.guestPhoneNumber}", arrivalDate: "${request.arrivalDate}",view: "${Constants.CONTENT}"}) 
              {
                    reservations {
                        modificationPermitted
                        cancellationPermitted
                        crsConfirmationNumber
                        itineraryNumber
                        externalReferenceNumber
                        id
                        channelConfirmationNumber
                        status
                        onHoldReleaseTime
                        onPropertyStatus
                        purposeOfStay
                        singleUsePaymentCardAllowed
                        sortOrder
                        roomStay{
                            numRooms
                            endDate
                            startDate
                            products{
                                startDate
                                endDate
                                primary
                                product{
                                    rateCode
                                    roomCode
                                    id
                                }
                            }
                            guestCount{
                                ageQualifyingCode
                                numGuests
                            }
                        }
                        hotel{
                            id
                            code
                            name
                        }
                        brand{
                            code
                            name
                        }                        
                        currency{
                            name
                            symbol
                            code
                        }
                        content{
                            roomCategories {
                                description
                                categoryCode
                                name    
                            }
                            rateCategories {
                                description
                                categoryCode
                                name   
                            }
                            rooms {
                                description
                                detailedDescription
                                categoryCode
                                code
                                name
                            }
                            rates {
                                description
                                detailedDescription
                                displayName
                                categoryCode
                                code
                                effectiveDate
                                expireDate
                                name
                                primary
                            }    
                        }
                        bookingDues {
                            noShowCharge {
                                amount
                            }
                            cancelPenalty{
                                amount
                                deadline
                                chargeList
                            }
                            deposit {
                                dueDate
                                amount
                                amountWithoutTax
                                status
                            }
                        }
                        notification {
                            sendGuestEmail
                            sendBookerEmail
                            bookingComment
                            deliveryComments {
                                comment
                            }
                        }
                        onPropertyInstructions {
                            chargeRoutingList
                        }
                        guests {
                            emailAddress {
                                type
                                value
                                default
                            }
                            contactNumbers {
                                number
                                code
                                default
                                sortOrder
                                type
                                role
                                use
                            }
                            marketingOptIn
                            role
                            startDate
                            endDate
                            payments {
                                role
                                type
                                vendorStatus
                                paymentCard {
                                    cardHolder
                                    cardCode
                                    cardName
                                    cardNumber
                                    token
                                    expireDate
                                }
                            }
                            personName {
                                prefix
                                firstName
                                lastName
                            }    
                        }
                        roomPrices {
                            averagePrice {
                                price {
                                    tax {
                                        amount
                                        breakDown {
                                            amount
                                            code
                                            name
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    fees {
                                        amount
                                         breakDown {
                                            amount
                                            code
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    displayOverrideAsPercentage
                                    originalAmount
                                    totalAmount
                                    amountPayableNow
                                    amountPayAtProperty
                                    totalAmountWithInclusiveTaxesFees
                                    totalAmountIncludingTaxesFees
                                    originalAmountIncludingTaxesAndFees
                                }
                            }
                            totalPrice {
                                price {
                                    tax {
                                        amount
                                        breakDown {
                                            amount
                                            code
                                            name
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    fees {
                                        amount
                                         breakDown {
                                            amount
                                            code
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    displayOverrideAsPercentage
                                    originalAmount
                                    totalAmount
                                    amountPayAtProperty
                                    totalAmountWithInclusiveTaxesFees
                                    totalAmountIncludingTaxesFees
                                    originalAmountIncludingTaxesAndFees
                                }
                            }
                            priceBreakdowns {
                                type
                                productPrices {
                                    startDate
                                    endDate
                                    product {
                                        rateCode
                                        roomCode
                                    }
                                    price {
                                        tax {
                                            amount
                                            breakDown {
                                                amount
                                                code
                                                name
                                                isPayAtProperty
                                                isInclusive
                                                isPerStay
                                            }
                                        }
                                        fees {
                                            amount
                                             breakDown {
                                                amount
                                                code
                                                isPayAtProperty
                                                isInclusive
                                                isPerStay
                                            }
                                        }
                                        displayOverrideAsPercentage
                                        originalAmount
                                        totalAmount
                                        amountPayableNow
                                        amountPayAtProperty
                                        totalAmountWithInclusiveTaxesFees
                                        totalAmountIncludingTaxesFees
                                        originalAmountIncludingTaxesAndFees
                                    }
                                }
                            }
                        }
                        overrides
                        discounts {
                            adjustmentAmount
                            adjustmentPercentage
                            type
                        }
                        cancelPolicy {
                            cancelFeeAmount {
                                taxInclusive
                                value
                            }
                            cancelFeeType
                            cancelPenaltyDate
                            cancelTime
                            cancelTimeIn
                            chargeType
                            chargeThreshold
                            description
                            modificationRestrictions
                            noShowFeeType
                            code
                            lateCancellationPermitted
                            charges {
                                daysToArrive
                                cancelFeeIncludesTax
                                cancelFeeAmount
                            }
                            noShowFeeAmount {
                                taxInclusive
                                value
                            }
                        }
                        bookingPolicy {
                            description
                            transactionFeeDisclaimer
                            guaranteeLevel
                            holdTime
                            allowPay
                            code
                            requirements
                            depositFee {
                                amount
                                dueDays
                                dueType
                                taxInclusive
                                isPrePayment
                                type
                            }
                        }
                        channels {
                            primaryChannel {
                                code
                            }
                        }                        
                    }
                    errorCode
                    message
                }
            }
        """.trimIndent()
                log.debug("_____Graphql query prepared for calling getBookingDetails API by mobile number_____")
                val jsonBody = createBooking.convertGraphQueryToJSONBody(query)
                log.debug("json query prepared as ${jsonBody.json}")
                callGetBookingDetails(jsonBody)
            }
            (!request.itineraryNumber.isNullOrEmpty())->{
                if(request.itineraryNumber.isEmpty()){
                    throw HttpResponseException(Constants.REQUIRED_MANDATORY_FIELDS, HttpStatusCode.BadRequest)
                }
                log.info("Calling getBookingDetails API with itinerary number")
                val query = """
              query {
              getHotelBookingDetails(getBooking: {hotelId:"e84bb5c3-b460-4b25-a2ee-9bb0c5221cfb", itineraryNumber: "${request.itineraryNumber}",view: "${Constants.CONTENT}"}) 
              {
                    reservations {
                        modificationPermitted
                        cancellationPermitted
                        crsConfirmationNumber
                        itineraryNumber
                        externalReferenceNumber
                        id
                        channelConfirmationNumber
                        status
                        onHoldReleaseTime
                        onPropertyStatus
                        purposeOfStay
                        singleUsePaymentCardAllowed
                        sortOrder
                        roomStay{
                            numRooms
                            endDate
                            startDate
                            products{
                                startDate
                                endDate
                                primary
                                product{
                                    rateCode
                                    roomCode
                                    id
                                }
                            }
                            guestCount{
                                ageQualifyingCode
                                numGuests
                            }
                        }
                        hotel{
                            id
                            code
                            name
                        }
                        brand{
                            code
                            name
                        }                        
                        currency{
                            name
                            symbol
                            code
                        }
                        content{
                            roomCategories {
                                description
                                categoryCode
                                name    
                            }
                            rateCategories {
                                description
                                categoryCode
                                name   
                            }
                            rooms {
                                description
                                detailedDescription
                                categoryCode
                                code
                                name
                            }
                            rates {
                                description
                                detailedDescription
                                displayName
                                categoryCode
                                code
                                effectiveDate
                                expireDate
                                name
                                primary
                            }    
                        }
                        bookingDues {
                            noShowCharge {
                                amount
                            }
                            cancelPenalty{
                                amount
                                deadline
                                chargeList
                            }
                            deposit {
                                dueDate
                                amount
                                amountWithoutTax
                                status
                            }
                        }
                        notification {
                            sendGuestEmail
                            sendBookerEmail
                            bookingComment
                            deliveryComments {
                                comment
                            }
                        }
                        onPropertyInstructions {
                            chargeRoutingList
                        }
                        guests {
                            emailAddress {
                                type
                                value
                                default
                            }
                            contactNumbers {
                                number
                                code
                                default
                                sortOrder
                                type
                                role
                                use
                            }
                            marketingOptIn
                            role
                            startDate
                            endDate
                            payments {
                                role
                                type
                                vendorStatus
                                paymentCard {
                                    cardHolder
                                    cardCode
                                    cardName
                                    cardNumber
                                    token
                                    expireDate
                                }
                            }
                            personName {
                                prefix
                                firstName
                                lastName
                            }    
                        }
                        roomPrices {
                            averagePrice {
                                price {
                                    tax {
                                        amount
                                        breakDown {
                                            amount
                                            code
                                            name
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    fees {
                                        amount
                                         breakDown {
                                            amount
                                            code
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    displayOverrideAsPercentage
                                    originalAmount
                                    totalAmount
                                    amountPayableNow
                                    amountPayAtProperty
                                    totalAmountWithInclusiveTaxesFees
                                    totalAmountIncludingTaxesFees
                                    originalAmountIncludingTaxesAndFees
                                }
                            }
                            totalPrice {
                                price {
                                    tax {
                                        amount
                                        breakDown {
                                            amount
                                            code
                                            name
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    fees {
                                        amount
                                         breakDown {
                                            amount
                                            code
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    displayOverrideAsPercentage
                                    originalAmount
                                    totalAmount
                                    amountPayAtProperty
                                    totalAmountWithInclusiveTaxesFees
                                    totalAmountIncludingTaxesFees
                                    originalAmountIncludingTaxesAndFees
                                }
                            }
                            priceBreakdowns {
                                type
                                productPrices {
                                    startDate
                                    endDate
                                    product {
                                        rateCode
                                        roomCode
                                    }
                                    price {
                                        tax {
                                            amount
                                            breakDown {
                                                amount
                                                code
                                                name
                                                isPayAtProperty
                                                isInclusive
                                                isPerStay
                                            }
                                        }
                                        fees {
                                            amount
                                             breakDown {
                                                amount
                                                code
                                                isPayAtProperty
                                                isInclusive
                                                isPerStay
                                            }
                                        }
                                        displayOverrideAsPercentage
                                        originalAmount
                                        totalAmount
                                        amountPayableNow
                                        amountPayAtProperty
                                        totalAmountWithInclusiveTaxesFees
                                        totalAmountIncludingTaxesFees
                                        originalAmountIncludingTaxesAndFees
                                    }
                                }
                            }
                        }
                        overrides
                        discounts {
                            adjustmentAmount
                            adjustmentPercentage
                            type
                        }
                        cancelPolicy {
                            cancelFeeAmount {
                                taxInclusive
                                value
                            }
                            cancelFeeType
                            cancelPenaltyDate
                            cancelTime
                            cancelTimeIn
                            chargeType
                            chargeThreshold
                            description
                            modificationRestrictions
                            noShowFeeType
                            code
                            lateCancellationPermitted
                            charges {
                                daysToArrive
                                cancelFeeIncludesTax
                                cancelFeeAmount
                            }
                            noShowFeeAmount {
                                taxInclusive
                                value
                            }
                        }
                        bookingPolicy {
                            description
                            transactionFeeDisclaimer
                            guaranteeLevel
                            holdTime
                            allowPay
                            code
                            requirements
                            depositFee {
                                amount
                                dueDays
                                dueType
                                taxInclusive
                                isPrePayment
                                type
                            }
                        }
                        channels {
                            primaryChannel {
                                code
                            }
                        }                        
                    }
                    errorCode
                    message
                }
            }
        """.trimIndent()
                log.info("_____Graphql query prepared for calling getBookingDetails API by itinerary number_____")
                val jsonBody = createBooking.convertGraphQueryToJSONBody(query)
                log.debug("json query prepared as ${jsonBody.json}")
                callGetBookingDetails(jsonBody)
            }
            else->{
                if(request.emailId.isNullOrEmpty()){
                    throw HttpResponseException(Constants.REQUIRED_MANDATORY_FIELDS, HttpStatusCode.BadRequest)
                }
                log.info("Calling getBookingDetails API with email id")
                val query = """
              query {
              getHotelBookingDetails(getBooking: {hotelId:"e84bb5c3-b460-4b25-a2ee-9bb0c5221cfb", guestEmail: "${request.emailId}", arrivalDate: "${request.arrivalDate}",view: "${Constants.CONTENT}"}) 
              {
                    reservations {
                        modificationPermitted
                        cancellationPermitted
                        crsConfirmationNumber
                        itineraryNumber
                        externalReferenceNumber
                        id
                        channelConfirmationNumber
                        status
                        onHoldReleaseTime
                        onPropertyStatus
                        purposeOfStay
                        singleUsePaymentCardAllowed
                        sortOrder
                        roomStay{
                            numRooms
                            endDate
                            startDate
                            products{
                                startDate
                                endDate
                                primary
                                product{
                                    rateCode
                                    roomCode
                                    id
                                }
                            }
                            guestCount{
                                ageQualifyingCode
                                numGuests
                            }
                        }
                        hotel{
                            id
                            code
                            name
                        }
                        brand{
                            code
                            name
                        }                        
                        currency{
                            name
                            symbol
                            code
                        }
                        content{
                            roomCategories {
                                description
                                categoryCode
                                name    
                            }
                            rateCategories {
                                description
                                categoryCode
                                name   
                            }
                            rooms {
                                description
                                detailedDescription
                                categoryCode
                                code
                                name
                            }
                            rates {
                                description
                                detailedDescription
                                displayName
                                categoryCode
                                code
                                effectiveDate
                                expireDate
                                name
                                primary
                            }    
                        }
                        bookingDues {
                            noShowCharge {
                                amount
                            }
                            cancelPenalty{
                                amount
                                deadline
                                chargeList
                            }
                            deposit {
                                dueDate
                                amount
                                amountWithoutTax
                                status
                            }
                        }
                        notification {
                            sendGuestEmail
                            sendBookerEmail
                            bookingComment
                            deliveryComments {
                                comment
                            }
                        }
                        onPropertyInstructions {
                            chargeRoutingList
                        }
                        guests {
                            emailAddress {
                                type
                                value
                                default
                            }
                            contactNumbers {
                                number
                                code
                                default
                                sortOrder
                                type
                                role
                                use
                            }
                            marketingOptIn
                            role
                            startDate
                            endDate
                            payments {
                                role
                                type
                                vendorStatus
                                paymentCard {
                                    cardHolder
                                    cardCode
                                    cardName
                                    cardNumber
                                    token
                                    expireDate
                                }
                            }
                            personName {
                                prefix
                                firstName
                                lastName
                            }    
                        }
                        roomPrices {
                            averagePrice {
                                price {
                                    tax {
                                        amount
                                        breakDown {
                                            amount
                                            code
                                            name
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    fees {
                                        amount
                                         breakDown {
                                            amount
                                            code
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    displayOverrideAsPercentage
                                    originalAmount
                                    totalAmount
                                    amountPayableNow
                                    amountPayAtProperty
                                    totalAmountWithInclusiveTaxesFees
                                    totalAmountIncludingTaxesFees
                                    originalAmountIncludingTaxesAndFees
                                }
                            }
                            totalPrice {
                                price {
                                    tax {
                                        amount
                                        breakDown {
                                            amount
                                            code
                                            name
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    fees {
                                        amount
                                         breakDown {
                                            amount
                                            code
                                            isPayAtProperty
                                            isInclusive
                                            isPerStay
                                        }
                                    }
                                    displayOverrideAsPercentage
                                    originalAmount
                                    totalAmount
                                    amountPayAtProperty
                                    totalAmountWithInclusiveTaxesFees
                                    totalAmountIncludingTaxesFees
                                    originalAmountIncludingTaxesAndFees
                                }
                            }
                            priceBreakdowns {
                                type
                                productPrices {
                                    startDate
                                    endDate
                                    product {
                                        rateCode
                                        roomCode
                                    }
                                    price {
                                        tax {
                                            amount
                                            breakDown {
                                                amount
                                                code
                                                name
                                                isPayAtProperty
                                                isInclusive
                                                isPerStay
                                            }
                                        }
                                        fees {
                                            amount
                                             breakDown {
                                                amount
                                                code
                                                isPayAtProperty
                                                isInclusive
                                                isPerStay
                                            }
                                        }
                                        displayOverrideAsPercentage
                                        originalAmount
                                        totalAmount
                                        amountPayableNow
                                        amountPayAtProperty
                                        totalAmountWithInclusiveTaxesFees
                                        totalAmountIncludingTaxesFees
                                        originalAmountIncludingTaxesAndFees
                                    }
                                }
                            }
                        }
                        overrides
                        discounts {
                            adjustmentAmount
                            adjustmentPercentage
                            type
                        }
                        cancelPolicy {
                            cancelFeeAmount {
                                taxInclusive
                                value
                            }
                            cancelFeeType
                            cancelPenaltyDate
                            cancelTime
                            cancelTimeIn
                            chargeType
                            chargeThreshold
                            description
                            modificationRestrictions
                            noShowFeeType
                            code
                            lateCancellationPermitted
                            charges {
                                daysToArrive
                                cancelFeeIncludesTax
                                cancelFeeAmount
                            }
                            noShowFeeAmount {
                                taxInclusive
                                value
                            }
                        }
                        bookingPolicy {
                            description
                            transactionFeeDisclaimer
                            guaranteeLevel
                            holdTime
                            allowPay
                            code
                            requirements
                            depositFee {
                                amount
                                dueDays
                                dueType
                                taxInclusive
                                isPrePayment
                                type
                            }
                        }
                        channels {
                            primaryChannel {
                                code
                            }
                        }                        
                    }
                    errorCode
                    message
                }
            }
            """.trimIndent()
                log.info("_____Graphql query prepared for calling getBookingDetails API by email id______")
                val jsonBody = createBooking.convertGraphQueryToJSONBody(query)
                log.debug("json query prepared as $jsonBody")
                callGetBookingDetails(jsonBody)
            }
        }
    }

    /* This method is used to call the hudini GetBookingDetails API using email id or confirmation number*/
    private suspend fun callGetBookingDetails(jsonBody: String): HttpResponse {
        log.info("callGetBookingDetails request $jsonBody")
        val token = generateToken.getToken()
        try {
            val response: HttpResponse = ConfigureHTTPClient.client.post(prop.bookingDevUrl) {
                timeout {
                    requestTimeoutMillis = prop.requestTimeoutMillis.toLong()
                }
                headers{
                    append(Constants.AUTHORIZATION,token)
                }
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
            }
            if(response.status== HttpStatusCode.Forbidden){
                generateToken.generateTokenAndSave()
                return callGetBookingDetails(jsonBody)
            }
            log.debug("Get Booking details response received from hudini is ${response.bodyAsText()}")
            return response
        } catch (e: Exception) {
            log.error("Exception occurred while calling api is ${e.message} due to ${e.stackTrace}")
            throw InternalServerException(e.message)
        }
    }
}