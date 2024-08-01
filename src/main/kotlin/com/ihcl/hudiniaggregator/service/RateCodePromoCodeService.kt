package com.ihcl.hudiniaggregator.service

import com.ihcl.hudiniaggregator.config.PropertiesConfiguration
import com.ihcl.hudiniaggregator.dto.checkAvailability.CheckAvailabilityInput
import com.ihcl.hudiniaggregator.dto.checkAvailability.CheckAvailabilityRateCodePromoCodeFERes
import com.ihcl.hudiniaggregator.dto.checkAvailability.CheckAvailabilityResponse
import com.ihcl.hudiniaggregator.dto.checkAvailability.ContentBookingPolicy
import com.ihcl.hudiniaggregator.dto.checkAvailability.ContentCancelPolicy
import com.ihcl.hudiniaggregator.dto.checkAvailability.RateCodePromoCodeRoomTypes
import com.ihcl.hudiniaggregator.dto.checkAvailability.RateCodePromoCodeRooms
import com.ihcl.hudiniaggregator.dto.checkAvailability.HudiniRoomType
import com.ihcl.hudiniaggregator.dto.checkAvailability.RateList
import com.ihcl.hudiniaggregator.dto.checkAvailability.Total
import com.ihcl.hudiniaggregator.dto.hotelAvailability.FEBreakDown
import com.ihcl.hudiniaggregator.dto.hotelAvailability.FETax
import com.ihcl.hudiniaggregator.exceptions.InternalServerException
import com.ihcl.hudiniaggregator.plugins.ConfigureHTTPClient
import com.ihcl.hudiniaggregator.util.Constants
import com.ihcl.hudiniaggregator.util.GenerateToken
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent
import org.litote.kmongo.json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RateCodePromoCodeService {
    private val prop = PropertiesConfiguration.env
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val createBooking by KoinJavaComponent.inject<CreateBookingService>(CreateBookingService::class.java)
    private val generateToken by KoinJavaComponent.inject<GenerateToken>(GenerateToken::class.java)
    val json = Json { ignoreUnknownKeys = true }

    /* This method is used to call hudini check availability API using rate code or promocde*/
    suspend fun getRateCodeOrPromoCode(request: CheckAvailabilityInput): CheckAvailabilityRateCodePromoCodeFERes? {
        log.info("Request received as $request")

        when {
            //rateCode
            (request.promoCode.isNullOrEmpty() && request.promoType.isNullOrEmpty() && request.agentId.isNullOrEmpty() && request.agentType.isNullOrEmpty() && request.couponCode.isNullOrEmpty()) -> {
                val token = generateToken.getToken()
                val query = """
                query {
                getHotelAvailability(availability: {hotelId:"${request.hotelId}", 
                startDate: "${request.startDate}", 
                endDate: "${request.endDate}",
                content: "${Constants.CONTENT}"
                numRooms: ${request.numRooms}, 
                adults: ${request.adults}, 
                children: ${request.children},
                onlyCheckRequested: "${Constants.ONLYCHECKREQUESTED}",
                rateCode: ["${request.rateCode}"],
                 }) 
                  {
                    roomAvailability {
                      hotel {
                        id
                      }
                      leastRestrictiveFailure {
                          additionalInformation
                          productStatus
                          date
                          level
                      }
                      roomTypes {
                        couponApplies
                        available
                        availableInventory
                        isMaximumPricedItem
                        isMinimumPricedItem
                        sortSequenceNumber
                        product {
                          bookingPolicy {
                            code
                          }
                          cancelPolicy {
                            code
                          }
                          prices {
                            daily {
                              price {
                                fees {
                                  amount
                                  breakDown {
                                     amount
                                     code
                                     isPayAtProperty
                                     isPerStay
                                     isInclusive
                                  }
                                }
                                tax {
                                  breakDown {
                                    amount
                                    code
                                    isPayAtProperty
                                    isPerStay
                                    isInclusive
                                  }
                                  amount
                                }
                                total {
                                  amount
                                  amountPayableNow
                                  amountPayAtProperty
                                  amountWithTaxesFees
                                  amountWithInclusiveTaxes
                                  amountWithFees
                                }
                                amount
                                currencyCode
                              }
                              date
                              availableInventory
                            }
                            perNight {
                              price {
                                fees {
                                  amount
                                }
                                tax {
                                  amount
                                }
                                total {
                                  amount
                                  amountWithTaxesFees
                                  amountWithInclusiveTaxes
                                  amountWithFees
                                }
                                amount
                                currencyCode
                              }
                            }
                            totalPrice {
                              price {
                                fees {
                                  breakDown {
                                     amount
                                     code
                                     isPayAtProperty
                                     isPerStay
                                     isInclusive
                                  }
                                  amount
                                }
                                tax {
                                  breakDown {
                                    amount
                                    code
                                    isPayAtProperty
                                    isPerStay
                                    isInclusive
                                  }
                                  amount
                                }
                                total {
                                  amount
                                  amountPayableNow
                                  amountWithTaxesFees
                                  amountWithInclusiveTaxes
                                  amountWithFees
                                }
                                amount
                                currencyCode
                              }
                            }
                            taxesFeesIncluded
                          }
                          rate {
                            code
                          }
                          room {
                            code
                          }
                          stayLimits {
                            minimumStay
                            maximumStay
                            maxStayThru
                          }
                          ref
                          refValue
                        }
                      }
                      productResult
                      hotel {
                        id
                      }
                    }
                     contentLists {
                roomList {
                    categoryCode
                    code
                    name
                    details {
                      description
                      detailedDescription
                      viewList {
                        description
                        code
                        isGDSPreferred
                        otaType
                      }
                      size {
                        max
                        min
                        units
                      }
                      indicators {
                        preferred
                      }
                      guestLimit {
                        adults
                        children
                        childrenIncluded
                        guestLimitTotal
                        value
                      }
                      featureList {
                        description
                        id
                        otaCode
                        otaType
                        sortOrder
                      }
                      extraBed {
                        allowed
                        cost
                      }
                      class {
                        code
                        description
                      }
                      bedding {
                        description
                        code
                        quantity
                        type
                        isPrimary
                      }
                    }
                  }
                  rateList {
                    categoryCode
                    code
                    currencyCode
                    name
                    details {
                      description
                      detailedDescription
                      displayName
                      displayDescription
                      rateClass
                  		indicators {
                        preferred
                        breakfastIncluded
                      }
            			channelAccessOverridesList
                    }
                  }
                   policyList {
                      bookingPolicy {
                          description
                          transactionFeeDisclaimer
                          guaranteeLevel
                          refundableStay
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
                              dueTime
                          }
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
                          charges
                          noShowFeeAmount{
                              taxInclusive
                              value
                          }
                      }
                   }
                   chargeList {
                        code
                        level
                        name
                        type
                        details {
                            description
                            frequency
                            chargePer
                            taxAmount
                            factorType
                            isInclusive
                            sortOrder
                        }
                    }
              }
                    errorCode
                    message 
                  }
                  }
            """.trimIndent()
                log.debug("_____GraphQl query prepared to call hudini check availability API by rateCode_____")
                val jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
                log.debug("converted query to json $jsonQuery")
                return callCheckAvailability(token, jsonQuery, request)
            }
            //promoCode
            (request.rateCode.isNullOrEmpty() && request.agentId.isNullOrEmpty() && request.agentType.isNullOrEmpty() && request.couponCode.isNullOrEmpty()) -> {
                val query = """
                query {
                getHotelAvailability(availability: {hotelId:"${request.hotelId}",
                  startDate: "${request.startDate}", 
                  endDate: "${request.endDate}",
                  content: "${Constants.CONTENT}"
                  numRooms: ${request.numRooms}, 
                  adults: ${request.adults}, 
                  children: ${request.children} , 
                  onlyCheckRequested: "${Constants.ONLYCHECKREQUESTED}",
                  promoCode: "${request.promoCode}", 
                  promoType: "${request.promoType}",
                }) 
                 {
                    roomAvailability {
                      hotel {
                        id
                      }
                      leastRestrictiveFailure {
                          additionalInformation
                          productStatus
                          date
                          level
                      }
                      roomTypes {
                        couponApplies
                        available
                        availableInventory
                        isMaximumPricedItem
                        isMinimumPricedItem
                        sortSequenceNumber
                        product {
                          bookingPolicy {
                            code
                          }
                          cancelPolicy {
                            code
                          }
                          prices {
                            daily {
                              price {
                                fees {
                                  amount
                                  breakDown {
                                     amount
                                     code
                                     isPayAtProperty
                                     isPerStay
                                     isInclusive
                                  }                                  
                                }
                                tax {
                                  breakDown {
                                    amount
                                    code
                                    isPayAtProperty
                                    isPerStay
                                    isInclusive
                                  }
                                  amount
                                }
                                total {
                                  amount
                                  amountPayableNow
                                  amountPayAtProperty
                                  amountWithTaxesFees
                                  amountWithInclusiveTaxes
                                  amountWithFees
                                }
                                amount
                                currencyCode
                              }
                              date
                              availableInventory
                            }
                            perNight {
                              price {
                                fees {
                                  amount
                                }
                                tax {
                                  amount
                                }
                                total {
                                  amount
                                  amountWithTaxesFees
                                  amountWithInclusiveTaxes
                                  amountWithFees
                                }
                                amount
                                currencyCode
                              }
                            }
                            totalPrice {
                              price {
                                fees {
                                  breakDown {
                                     amount
                                     code
                                     isPayAtProperty
                                     isPerStay
                                     isInclusive
                                  }
                                  amount
                                }
                                tax {
                                  breakDown {
                                    amount
                                    code
                                    isPayAtProperty
                                    isPerStay
                                    isInclusive
                                  }
                                  amount
                                }
                                total {
                                  amount
                                  amountPayableNow
                                  amountWithTaxesFees
                                  amountWithInclusiveTaxes
                                  amountWithFees
                                }
                                amount
                                currencyCode
                              }
                            }
                            taxesFeesIncluded
                          }
                          rate {
                            code
                          }
                          room {
                            code
                          }
                          stayLimits {
                            minimumStay
                            maximumStay
                            maxStayThru
                          }
                           ref
                          refValue
                        }
                      }
                      hotel {
                        id
                      }
                    }
                     contentLists {
                roomList {
                    categoryCode
                    code
                    name
                    details {
                      description
                      detailedDescription
                      viewList {
                        description
                        code
                        isGDSPreferred
                        otaType
                      }
                      size {
                        max
                        min
                        units
                      }
                      indicators {
                        preferred
                      }
                      guestLimit {
                        adults
                        children
                        childrenIncluded
                        guestLimitTotal
                        value
                      }
                      featureList {
                        description
                        id
                        otaCode
                        otaType
                        sortOrder
                      }
                      extraBed {
                        allowed
                        cost
                      }
                      class {
                        code
                        description
                      }
                      bedding {
                        description
                        code
                        quantity
                        type
                        isPrimary
                      }
                    }
                  }
                  rateList {
                    categoryCode
                    code
                    currencyCode
                    name
                    details {
                      description
                      detailedDescription
                      displayName
                      displayDescription
                      rateClass
                  		indicators {
                        preferred
                        breakfastIncluded
                      }
            			channelAccessOverridesList
                    }
                  }
                   policyList {
                      bookingPolicy {
                          description
                          transactionFeeDisclaimer
                          guaranteeLevel
                          refundableStay
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
                              dueTime
                          }
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
                          charges
                          noShowFeeAmount{
                              taxInclusive
                              value
                          }
                      }
                   }
                   chargeList {
                        code
                        level
                        name
                        type
                        details {
                            description
                            frequency
                            chargePer
                            taxAmount
                            factorType
                            isInclusive
                            sortOrder
                        }
                    }
              }
                    errorCode
                    message 
                  }
                  }
            """.trimIndent()
                val token = generateToken.getToken()
                log.debug("_____GraphQl query prepared to call hudini check availability API by promoCode_____")
                val jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
                log.debug("converted query to json $jsonQuery")
                return callCheckAvailability(token, jsonQuery, request)
            }
            //agentCode
            (request.rateCode.isNullOrEmpty() && request.promoCode.isNullOrEmpty() && request.promoType.isNullOrEmpty() && request.couponCode.isNullOrEmpty()) -> {
                val query = """
                query {
                getHotelAvailability(availability: {hotelId:"${request.hotelId}",
                  startDate: "${request.startDate}", 
                  endDate: "${request.endDate}",
                  content: "${Constants.CONTENT}"
                  numRooms: ${request.numRooms}, 
                  adults: ${request.adults}, 
                  children: ${request.children} , 
                  onlyCheckRequested: "${Constants.ONLYCHECKREQUESTED}",
                  agentId: ${request.agentId}, 
                  agentType: "${request.agentType}",
                }) 
                 {
                    roomAvailability {
                      hotel {
                        id
                      }
                      leastRestrictiveFailure {
                          additionalInformation
                          productStatus
                          date
                          level
                      }
                      roomTypes {
                        couponApplies
                        available
                        availableInventory
                        isMaximumPricedItem
                        isMinimumPricedItem
                        sortSequenceNumber
                        product {
                          bookingPolicy {
                            code
                          }
                          cancelPolicy {
                            code
                          }
                          prices {
                            daily {
                              price {
                                fees {
                                  amount
                                  breakDown {
                                     amount
                                     code
                                     isPayAtProperty
                                     isPerStay
                                     isInclusive
                                  }                                  
                                }
                                tax {
                                  breakDown {
                                    amount
                                    code
                                    isPayAtProperty
                                    isPerStay
                                    isInclusive
                                  }
                                  amount
                                }
                                total {
                                  amount
                                  amountPayableNow
                                  amountPayAtProperty
                                  amountWithTaxesFees
                                  amountWithInclusiveTaxes
                                  amountWithFees
                                }
                                amount
                                currencyCode
                              }
                              date
                              availableInventory
                            }
                            perNight {
                              price {
                                fees {
                                  amount
                                }
                                tax {
                                  amount
                                }
                                total {
                                  amount
                                  amountWithTaxesFees
                                  amountWithInclusiveTaxes
                                  amountWithFees
                                }
                                amount
                                currencyCode
                              }
                            }
                            totalPrice {
                              price {
                                fees {
                                  breakDown {
                                     amount
                                     code
                                     isPayAtProperty
                                     isPerStay
                                     isInclusive
                                  }
                                  amount
                                }
                                tax {
                                  breakDown {
                                    amount
                                    code
                                    isPayAtProperty
                                    isPerStay
                                    isInclusive
                                  }
                                  amount
                                }
                                total {
                                  amount
                                  amountPayableNow
                                  amountWithTaxesFees
                                  amountWithInclusiveTaxes
                                  amountWithFees
                                }
                                amount
                                currencyCode
                              }
                            }
                            taxesFeesIncluded
                          }
                          rate {
                            code
                          }
                          room {
                            code
                          }
                          stayLimits {
                            minimumStay
                            maximumStay
                            maxStayThru
                          }
                           ref
                          refValue
                        }
                      }
                      hotel {
                        id
                      }
                    }
                     contentLists {
                roomList {
                    categoryCode
                    code
                    name
                    details {
                      description
                      detailedDescription
                      viewList {
                        description
                        code
                        isGDSPreferred
                        otaType
                      }
                      size {
                        max
                        min
                        units
                      }
                      indicators {
                        preferred
                      }
                      guestLimit {
                        adults
                        children
                        childrenIncluded
                        guestLimitTotal
                        value
                      }
                      featureList {
                        description
                        id
                        otaCode
                        otaType
                        sortOrder
                      }
                      extraBed {
                        allowed
                        cost
                      }
                      class {
                        code
                        description
                      }
                      bedding {
                        description
                        code
                        quantity
                        type
                        isPrimary
                      }
                    }
                  }
                  rateList {
                    categoryCode
                    code
                    currencyCode
                    name
                    details {
                      description
                      detailedDescription
                      displayName
                      displayDescription
                      rateClass
                  		indicators {
                        preferred
                        breakfastIncluded
                      }
            			channelAccessOverridesList
                    }
                  }
                   policyList {
                      bookingPolicy {
                          description
                          transactionFeeDisclaimer
                          guaranteeLevel
                          refundableStay
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
                              dueTime
                          }
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
                          charges
                          noShowFeeAmount{
                              taxInclusive
                              value
                          }
                      }
                   }
                   chargeList {
                        code
                        level
                        name
                        type
                        details {
                            description
                            frequency
                            chargePer
                            taxAmount
                            factorType
                            isInclusive
                            sortOrder
                        }
                    }
              }
                    errorCode
                    message 
                  }
                  }
            """.trimIndent()
                val token = generateToken.getToken()
                log.debug("_____GraphQl query prepared to call hudini check availability API by agentId_____")
                val jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
                log.debug("converted query to json $jsonQuery")
                return callCheckAvailability(token, jsonQuery, request)
            }
            //couponCode
            (request.couponCode != null) -> {
                val query = """
                query {
                getHotelAvailability(availability: {hotelId:"${request.hotelId}",
                  startDate: "${request.startDate}", 
                  endDate: "${request.endDate}",
                  content: "${Constants.CONTENT}"
                  numRooms: ${request.numRooms}, 
                  adults: ${request.adults}, 
                  children: ${request.children} ,
                  couponCode: ["${request.couponCode}"]
                }) 
                 {
                    roomAvailability {
                      hotel {
                        id
                      }
                      leastRestrictiveFailure {
                          additionalInformation
                          productStatus
                          date
                          level
                      }
                      roomTypes {
                        couponApplies
                        available
                        availableInventory
                        isMaximumPricedItem
                        isMinimumPricedItem
                        sortSequenceNumber
                        product {
                          bookingPolicy {
                            code
                          }
                          cancelPolicy {
                            code
                          }
                          prices {
                            daily {
                              price {
                                fees {
                                  amount
                                  breakDown {
                                     amount
                                     code
                                     isPayAtProperty
                                     isPerStay
                                     isInclusive
                                  }                                  
                                }
                                tax {
                                  breakDown {
                                    amount
                                    code
                                    isPayAtProperty
                                    isPerStay
                                    isInclusive
                                  }
                                  amount
                                }
                                total {
                                  amount
                                  amountPayableNow
                                  amountPayAtProperty
                                  amountWithTaxesFees
                                  amountWithInclusiveTaxes
                                  amountWithFees
                                }
                                amount
                                currencyCode
                              }
                              date
                              availableInventory
                            }
                            perNight {
                              price {
                                fees {
                                  amount
                                }
                                tax {
                                  amount
                                }
                                total {
                                  amount
                                  amountWithTaxesFees
                                  amountWithInclusiveTaxes
                                  amountWithFees
                                }
                                amount
                                currencyCode
                              }
                            }
                            totalPrice {
                              price {
                                fees {
                                    breakDown {
                                     amount
                                     code
                                     isPayAtProperty
                                     isPerStay
                                     isInclusive
                                  }
                                  amount
                                }
                                tax {
                                  breakDown {
                                    amount
                                    code
                                    isPayAtProperty
                                    isPerStay
                                    isInclusive
                                  }
                                  amount
                                }
                                total {
                                  amount
                                  amountPayableNow
                                  amountWithTaxesFees
                                  amountWithInclusiveTaxes
                                  amountWithFees
                                }
                                amount
                                currencyCode
                              }
                            }
                            taxesFeesIncluded
                          }
                          rate {
                            code
                          }
                          room {
                            code
                          }
                          stayLimits {
                            minimumStay
                            maximumStay
                            maxStayThru
                          }
                           ref
                          refValue
                        }
                      }
                      hotel {
                        id
                      }
                    }
                    warning{
                        code
                        value
                    }
                     contentLists {
                roomList {
                    categoryCode
                    code
                    name
                    details {
                      description
                      detailedDescription
                      viewList {
                        description
                        code
                        isGDSPreferred
                        otaType
                      }
                      size {
                        max
                        min
                        units
                      }
                      indicators {
                        preferred
                      }
                      guestLimit {
                        adults
                        children
                        childrenIncluded
                        guestLimitTotal
                        value
                      }
                      featureList {
                        description
                        id
                        otaCode
                        otaType
                        sortOrder
                      }
                      extraBed {
                        allowed
                        cost
                      }
                      class {
                        code
                        description
                      }
                      bedding {
                        description
                        code
                        quantity
                        type
                        isPrimary
                      }
                    }
                  }
                  rateList {
                    categoryCode
                    code
                    currencyCode
                    name
                    details {
                      description
                      detailedDescription
                      displayName
                      displayDescription
                      rateClass
                  		indicators {
                        preferred
                        breakfastIncluded
                      }
            			channelAccessOverridesList
                    }
                  }
                   policyList {
                      bookingPolicy {
                          description
                          transactionFeeDisclaimer
                          guaranteeLevel
                          refundableStay
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
                              dueTime
                          }
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
                          charges
                          noShowFeeAmount{
                              taxInclusive
                              value
                          }
                      }
                   }
                   chargeList {
                        code
                        level
                        name
                        type
                        details {
                            description
                            frequency
                            chargePer
                            taxAmount
                            factorType
                            isInclusive
                            sortOrder
                        }
                    }
              }
                    errorCode
                    message 
                  }
                  }
            """.trimIndent()
                val token = generateToken.getToken()
                log.debug("_____GraphQl query prepared to call hudini check availability API by coupon code_____")
                val jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
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
                        return getRateCodeOrPromoCode(request)
                    }
                    val res = response.body<CheckAvailabilityResponse>()
                    log.info("agent Code response received from hudini is ${response.bodyAsText()}")
                    return populateCouponCodeResponse(res, request)
                } catch (e: Exception) {
                    log.error("Exception occurred while calling hotel availability api is ${e.message} due to ${e.stackTrace}")
                    throw InternalServerException(e.message)
                }
            }
        }

        return null
    }
    private suspend fun callCheckAvailability(
        token: String,
        jsonQuery: String,
        request: CheckAvailabilityInput,
    ): CheckAvailabilityRateCodePromoCodeFERes? {
        log.info("callCheckAvailability request: ${request.hotelId}")
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
                return getRateCodeOrPromoCode(request)
            }
            val res = response.body<CheckAvailabilityResponse>()
            log.info("Promo Code response received from hudini is ${res.json}")
            return populateRateCodeAndPromoCodeResponse(request.hotelId!!, res)
        } catch (e: Exception) {
            log.error("Exception occurred while calling hotel availability api is ${e.message} due to ${e.stackTrace}")
            throw InternalServerException(e.message)
        }
    }

    private fun populateRateCodeAndPromoCodeResponse(
        hotelId: String,
        response: CheckAvailabilityResponse,
    ): CheckAvailabilityRateCodePromoCodeFERes {
        log.info("populateRateCodeAndPromoCodeResponse hotelId: $hotelId")
        val memberRoomCode = mutableSetOf<String>()
        val memberRateCode = mutableSetOf<String>()

        response.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rc ->
            memberRoomCode.add(
                rc.product?.room?.code!!
            )
            memberRateCode.add(
                rc.product.rate?.code!!
            )
        }
        log.debug("Total room codes {}", memberRoomCode)
        log.debug("Total rate codes {}", memberRateCode)

        val rooms = mutableListOf<RateCodePromoCodeRoomTypes>()
        //mapping room content and rooms to particular room code
        //creating room type objects
        for (i in 0 until memberRoomCode.size) {
            rooms.add(
                RateCodePromoCodeRoomTypes(
                    roomCode = memberRoomCode.toList()[i],
                    rooms = mutableListOf()
                )
            )
        }
        rooms.forEach {
            response.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rt ->
                response.data.getHotelAvailability.contentLists?.rateList?.forEach { rateContent ->
                    response.data.getHotelAvailability.contentLists.policyList?.bookingPolicy?.forEach { bookingPolicy ->
                        response.data.getHotelAvailability.contentLists.policyList.cancelPolicy?.forEach { cancellationPolicy ->
                            if (rateContent.code == rt.product?.rate?.code && bookingPolicy?.code == rt.product?.bookingPolicy?.code && cancellationPolicy?.code == rt.product?.cancelPolicy?.code && it.roomCode == rt.product?.room?.code) {
                                it.rooms?.add(mapRoomDetails(rt, rateContent, bookingPolicy!!, cancellationPolicy!!))
                            }
                        }
                    }
                }
            }
        }
        //response prepared with member rates
        return CheckAvailabilityRateCodePromoCodeFERes(
            hotelId,
            response.data?.getHotelAvailability?.roomAvailability?.hotel?.id,
            rooms,
            null,
            response.data?.getHotelAvailability?.contentLists?.chargeList,
            response.data?.getHotelAvailability?.roomAvailability?.leastRestrictiveFailure,
            false,
            null,
            response.data?.getHotelAvailability?.message,
            response.data?.getHotelAvailability?.errorCode,
        )
    }

    private fun populateCouponCodeResponse(
        response: CheckAvailabilityResponse,
        request: CheckAvailabilityInput,
    ): CheckAvailabilityRateCodePromoCodeFERes {
        log.info("populateCouponCodeResponse hotelId: ${request.hotelId}")
        val couponCodeResponse: CheckAvailabilityRateCodePromoCodeFERes?
        val memberRoomCode = mutableSetOf<String>()
        val memberRateCode = mutableSetOf<String>()

        response.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rc ->
            memberRoomCode.add(
                rc.product?.room?.code!!
            )
            memberRateCode.add(
                rc.product.rate?.code!!
            )
        }
        log.debug("Total room codes {}", memberRoomCode)
        log.debug("Total rate codes {}", memberRateCode)

        if (response.data?.getHotelAvailability?.warning != null) {
            couponCodeResponse = CheckAvailabilityRateCodePromoCodeFERes(
                request.hotelId,
                response.data.getHotelAvailability.roomAvailability?.hotel?.id,
                null,
                request.couponCode,
                response.data.getHotelAvailability.contentLists?.chargeList,
                response.data.getHotelAvailability.roomAvailability?.leastRestrictiveFailure,
                false,
                Constants.INVALID_COUPON_CODE,
                response.data.getHotelAvailability.message,
                response.data.getHotelAvailability.errorCode,
            )
            return couponCodeResponse
        }

        val rooms = mutableListOf<RateCodePromoCodeRoomTypes>()
        //mapping room content and rooms to particular room code
        //creating room type objects
        for (i in 0 until memberRoomCode.size) {
            rooms.add(
                RateCodePromoCodeRoomTypes(
                    roomCode = memberRoomCode.toList()[i],
                    rooms = mutableListOf()
                )
            )
        }
        rooms.forEach {
            response.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rt ->
                if (rt.couponApplies == true) {
                    response.data.getHotelAvailability.contentLists?.rateList?.forEach { rateContent ->
                        response.data.getHotelAvailability.contentLists.policyList?.bookingPolicy?.forEach { bookingPolicy ->
                            response.data.getHotelAvailability.contentLists.policyList.cancelPolicy?.forEach { cancellationPolicy ->
                                if (rateContent.code == rt.product?.rate?.code && bookingPolicy?.code == rt.product?.bookingPolicy?.code && cancellationPolicy?.code == rt.product?.cancelPolicy?.code && it.roomCode == rt.product?.room?.code) {
                                    it.rooms?.add(
                                        mapRoomDetails(
                                            rt,
                                            rateContent,
                                            bookingPolicy!!,
                                            cancellationPolicy!!
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        val numOfAvailRooms = arrayListOf<String>()
        rooms.forEach {
            if (it.rooms?.isEmpty() == true) {
                numOfAvailRooms.add(it.roomCode!!)
            }
        }
        return mapCouponCodeResponse(rooms, numOfAvailRooms, request, response)
    }

    private fun mapCouponCodeResponse(
        rooms: MutableList<RateCodePromoCodeRoomTypes>,
        numOfAvailRooms: ArrayList<String>,
        request: CheckAvailabilityInput,
        response: CheckAvailabilityResponse,
    ): CheckAvailabilityRateCodePromoCodeFERes {
        log.info("mapCouponCodeResponse hotelId: ${request.hotelId}")
        val couponCodeResponse: CheckAvailabilityRateCodePromoCodeFERes?
        return when {
            (rooms.size == numOfAvailRooms.size) -> {
                couponCodeResponse = CheckAvailabilityRateCodePromoCodeFERes(
                    request.hotelId,
                    response.data?.getHotelAvailability?.roomAvailability?.hotel?.id,
                    rooms,
                    request.couponCode,
                    response.data?.getHotelAvailability?.contentLists?.chargeList,
                    response.data?.getHotelAvailability?.roomAvailability?.leastRestrictiveFailure,
                    false,
                    Constants.USED_VALID_COUPON,
                    response.data?.getHotelAvailability?.message,
                    response.data?.getHotelAvailability?.errorCode,
                )
                couponCodeResponse
            }

            else -> {
                couponCodeResponse = CheckAvailabilityRateCodePromoCodeFERes(
                    request.hotelId,
                    response.data?.getHotelAvailability?.roomAvailability?.hotel?.id,
                    rooms,
                    request.couponCode,
                    response.data?.getHotelAvailability?.contentLists?.chargeList,
                    response.data?.getHotelAvailability?.roomAvailability?.leastRestrictiveFailure,
                    true,
                    Constants.COUPON_CODE_APPLIED_SUCCESSFULLY,
                    response.data?.getHotelAvailability?.message,
                    response.data?.getHotelAvailability?.errorCode,
                )
                couponCodeResponse
            }
        }
    }

    private fun mapRoomDetails(
        response: HudiniRoomType,
        rateList: RateList?,
        booingPolicy: ContentBookingPolicy?,
        cancellationPolicy: ContentCancelPolicy?,
    ): RateCodePromoCodeRooms {
        val breakDown: MutableList<FEBreakDown> = response.product?.prices?.totalPrice?.price?.tax?.breakDown!!.map {
            FEBreakDown(
                amount = it?.amount,
                code = it?.code,
                isInclusive = it?.isInclusive,
                isPayAtProperty = it?.isPayAtProperty,
                isPerStay = it?.isPerStay
            )
        }.toMutableList()

        return RateCodePromoCodeRooms(
            available = response.available,
            availableInventory = response.availableInventory,
            rateCode = response.product.rate?.code!!,
            rateContent = rateList,
            bookingPolicy = booingPolicy,
            cancellationPolicy = cancellationPolicy,
            daily = response.product.prices.daily,
            perNight = response.product.prices.perNight,
            tax = FETax(
                amount = response.product.prices.totalPrice.price.tax.amount!!,
                breakDown = breakDown
            ),
            total = Total(
                amount = response.product.prices.totalPrice.price.total?.amount,
                amountPayableNow = response.product.prices.totalPrice.price.total?.amountPayableNow,
                amountWithInclusiveTaxes = response.product.prices.totalPrice.price.total?.amountWithInclusiveTaxes,
                amountWithTaxesFees = response.product.prices.totalPrice.price.total?.amountWithTaxesFees,
                amountPayAtProperty = response.product.prices.totalPrice.price.total?.amountPayAtProperty,
                amountWithFees = response.product.prices.totalPrice.price.total?.amountWithFees
            )
        )
    }
}


