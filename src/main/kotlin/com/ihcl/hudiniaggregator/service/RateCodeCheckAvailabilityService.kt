package com.ihcl.hudiniaggregator.service

import com.ihcl.hudiniaggregator.config.PropertiesConfiguration
import com.ihcl.hudiniaggregator.dto.checkAvailability.*
import com.ihcl.hudiniaggregator.dto.hotelAvailability.FEBreakDown
import com.ihcl.hudiniaggregator.dto.hotelAvailability.FETax
import com.ihcl.hudiniaggregator.exceptions.HttpResponseException
import com.ihcl.hudiniaggregator.plugins.ConfigureHTTPClient
import com.ihcl.hudiniaggregator.util.Constants
import com.ihcl.hudiniaggregator.util.GenerateToken
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.koin.java.KoinJavaComponent
import org.litote.kmongo.json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RateCodeCheckAvailabilityService {
    private val prop = PropertiesConfiguration.env
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val createBooking by KoinJavaComponent.inject<CreateBookingService>(CreateBookingService::class.java)
    private val generateToken by KoinJavaComponent.inject<GenerateToken>(GenerateToken::class.java)

    suspend fun checkRateCodeAvailability(request: ChangeDatesCheckAvailabilityRequest): ChangeDatesCheckAvailabilityResponse? {
        log.debug("Request received to check rate code availability for changing dates is ${request.json}")
        val roomTypes: MutableList<ChangeDatesRateCodePromoCodeRoomTypes> = mutableListOf()
        val roomNumbers = mutableListOf<Int>()
        val hudiniRequest = mutableListOf<String>()
        var feRes: ChangeDatesCheckAvailabilityResponse? = null

        //collect all the room numbers in roomNumbers based on which number of objects with room number will be created in response.
        request.rooms.forEach {
            roomNumbers.add(it.roomNumber)
        }
        log.debug("Total room numbers are {}", roomNumbers)

        //Prepare hudini query for all the request and collect in hudiniRequest variable
        request.rooms.forEach { room ->
            hudiniRequest.add(prepareHudiniRequest(request, room))
        }
        log.debug("hudini request ${hudiniRequest.json}")
        //pass all the request and get all the responses
        val hudiniResponses = getAllResponses(hudiniRequest)
        log.debug("List of all the response {}", hudiniResponses.json)

        //create a response structure of the API based on num of rooms
        hudiniResponses.withIndex().find {(_,checkAvailability) ->
            checkAvailability.data?.getHotelAvailability?.roomAvailability?.roomTypes?.isNotEmpty() == true
        }?.let {
            feRes = populateChangeRatesResponse(hudiniResponses[it.index], roomNumbers, request)
        }?:run{
            throw HttpResponseException(Constants.NO_ROOMS_AVAILABLE_FOR_SELECTED_DATES,HttpStatusCode.NotFound)
        }

        val listRestrictiveFailure = mutableListOf<LeastRestrictiveFailure?>()
        hudiniResponses.forEach {
            listRestrictiveFailure.add(it.data?.getHotelAvailability?.roomAvailability?.leastRestrictiveFailure)
        }

        //bind all the responses to the roomTypes object and assign the object to API response structure created in last step and return the response.
        for ((index, value) in roomNumbers.withIndex()) {
            feRes?.roomTypes?.forEach {
                if (it.roomNumber == value) {
                    roomTypes.add(prepareRoomTypes(value, hudiniResponses[index], it)!!)
                }
            }
        }
        feRes?.roomTypes = roomTypes
        return feRes
    }

    //This fun binds the roomType object to rooms.
    private fun prepareRoomTypes(
        roomNumber: Int,
        rateCodeResponse: CheckAvailabilityResponse,
        feRes: ChangeDatesRateCodePromoCodeRoomTypes
    ): ChangeDatesRateCodePromoCodeRoomTypes? {
        var roomType: ChangeDatesRateCodePromoCodeRoomTypes? = null
        if (feRes.roomNumber == roomNumber) {
            roomType = ChangeDatesRateCodePromoCodeRoomTypes(
                roomNumber = roomNumber,
                rooms = mutableListOf(),
                chargeList = rateCodeResponse.data?.getHotelAvailability?.contentLists?.chargeList,
                leastRestrictiveFailure = rateCodeResponse.data?.getHotelAvailability?.roomAvailability?.leastRestrictiveFailure
            )
            rateCodeResponse.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rt ->
                rateCodeResponse.data.getHotelAvailability.contentLists?.rateList?.forEach { rateContent ->
                    rateCodeResponse.data.getHotelAvailability.contentLists.policyList?.bookingPolicy?.forEach { bookingPolicy ->
                        rateCodeResponse.data.getHotelAvailability.contentLists.policyList.cancelPolicy?.forEach { cancellationPolicy ->
                            if ((rateContent.code == rt.product?.rate?.code) && (bookingPolicy?.code == rt.product?.bookingPolicy?.code) && (cancellationPolicy?.code == rt.product?.cancelPolicy?.code)) {
                                roomType.rooms?.add(
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
        return roomType
    }

    //This fun maps all the room content and prices to room.
    private fun mapRoomDetails(
        response: HudiniRoomType,
        rateList: RateList,
        booingPolicy: ContentBookingPolicy,
        cancellationPolicy: ContentCancelPolicy
    ): ChangeDatesRateCodePromoCodeRooms {
        val breakDown: MutableList<FEBreakDown> =
            response.product?.prices?.totalPrice?.price?.tax?.breakDown!!.map {
                FEBreakDown(
                    amount = it?.amount,
                    code = it?.code,
                    isInclusive = it?.isInclusive,
                    isPayAtProperty = it?.isPayAtProperty,
                    isPerStay = it?.isPerStay
                )
            }.toMutableList()

        return ChangeDatesRateCodePromoCodeRooms(
            available = response.available,
            availableInventory = response.availableInventory,
            roomCode = response.product.room?.code,
            rateCode = response.product.rate?.code,
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

    //This method prepares the structure of the response
    private fun populateChangeRatesResponse(
        response: CheckAvailabilityResponse,
        roomNumbers: MutableList<Int>,
        request: ChangeDatesCheckAvailabilityRequest
    ): ChangeDatesCheckAvailabilityResponse {
        val memberRoomCode = mutableSetOf<String>()

        response.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rc ->
            memberRoomCode.add(
                rc.product?.room?.code!!
            )
        }

        log.debug("Total room codes {}", memberRoomCode)

        val rooms = mutableListOf<ChangeDatesRateCodePromoCodeRoomTypes>()
        log.debug("number of rooms {}", roomNumbers)
        //creating rooms object for each roomNumber
        response.data?.getHotelAvailability?.contentLists?.roomList?.forEach { roomContent ->
            //creating room type objects
            for (i in 0 until roomNumbers.size) {
                if (memberRoomCode.toList()[i] == roomContent.code) {
                    rooms.add(
                        ChangeDatesRateCodePromoCodeRoomTypes(
                            roomNumber = roomNumbers.toList()[i],
                            rooms = mutableListOf(),
                            chargeList = response.data.getHotelAvailability.contentLists.chargeList,
                            leastRestrictiveFailure = response.data.getHotelAvailability.roomAvailability?.leastRestrictiveFailure
                        )
                    )
                }
            }
        }


        return ChangeDatesCheckAvailabilityResponse(
            request.hotelId,
            rooms
        )
    }

    //This method prepares and returns the GraphQL query and converts to json body.
    private fun prepareHudiniRequest(
        request: ChangeDatesCheckAvailabilityRequest,
        room: ChangeDatesRoomsRequest
    ): String {
        val query = """
                query {
                getHotelAvailability(availability: {hotelId:"${request.hotelId}",
                startDate: "${request.checkInDate}",
                endDate: "${request.checkOutDate}",
                numRooms: 1,
                adults: ${room.adults},
                children: ${room.children},
                content: "${Constants.CONTENT}",
                onlyCheckRequested: "${Constants.ONLYCHECKREQUESTED}",
                rateCode: ["${room.rateCode}"],
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
                                }
                                amount
                                currencyCode
                              }
                            }
                            totalPrice {
                              price {
                                fees {
                                 	breakDown
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
        return jsonQuery
    }

    //This method accepts the list of request and internally makes call to hudini api with each request prepares and returns the list of all the responses.
    private suspend fun getAllResponses(
        jsonQueryList: MutableList<String>
    ): MutableList<CheckAvailabilityResponse> {
        val results = mutableListOf<CheckAvailabilityResponse>()
        jsonQueryList.forEach { value ->
            results.add(callRateCodeAvailability(value))
        }
        return results
    }

    //This method makes the call to hudini API.
    private suspend fun callRateCodeAvailability(jsonQuery: String): CheckAvailabilityResponse {
        log.info("callRateCodeAvailability request: $jsonQuery")
        val token = generateToken.getToken()
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
            return callRateCodeAvailability(jsonQuery)
        }
        val res = response.body<CheckAvailabilityResponse>()
        log.info("Rate Code response received from hudini is ${res.json}")
        return res
    }
}