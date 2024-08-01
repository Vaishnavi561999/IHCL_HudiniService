package com.ihcl.hudiniaggregator.service

import com.ihcl.hudiniaggregator.config.PropertiesConfiguration
import com.ihcl.hudiniaggregator.dto.*
import com.ihcl.hudiniaggregator.exceptions.HttpResponseException
import com.ihcl.hudiniaggregator.exceptions.InternalServerException
import com.ihcl.hudiniaggregator.plugins.ConfigureHTTPClient
import com.ihcl.hudiniaggregator.util.Constants
import com.ihcl.hudiniaggregator.util.GenerateToken
import com.ihcl.hudiniaggregator.util.validateDateFormat
import io.ktor.client.call.*
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import org.koin.java.KoinJavaComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.ktor.client.request.headers
import org.litote.kmongo.json



class GetHotelLeadAvailabilityCalenderService {
    private val createBooking by KoinJavaComponent.inject<CreateBookingService>(CreateBookingService::class.java)
    private val generateToken by KoinJavaComponent.inject<GenerateToken>(GenerateToken::class.java)
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val prop = PropertiesConfiguration.env
    suspend fun getHotelLeadAvailabilityCalender(req: GetHotelLeadAvailabilityCalenderReq): Any {
        if (req.hotelId.isNullOrEmpty() || req.startDate.isNullOrEmpty() || req.endDate.isNullOrEmpty()) {
            throw HttpResponseException("Mandatory fields are missing!!", HttpStatusCode.BadRequest)
        }
        validateDateFormat(req.startDate, req.endDate)
        return when {
            (!req.rateCode.isNullOrEmpty()) -> {
                log.info("Lead availability with rate code request")
                val query = """
            query {
            getHotelLeadAvailability(leadAvailability: {
             hotelId:"${req.hotelId}",
             startDate:"${req.startDate}", 
             endDate:"${req.endDate}",
              numRooms: 1, 
              adults: 1, 
              children: 0, 
              onlyCheckRequested:"${Constants.ONLYCHECKREQUESTED}",
              lengthOfStay: ${req.lengthOfStay},
              rateCode:["${req.rateCode}"]
                }) 
              {
                leadAvailability {
                  leastRestrictiveFailure {
                    level
                    cause
                    overrideAllowed
                  }
                  failures {
                      cause
                      percentage
                  }
                  available
                  arrivalDate
                  departureDate
                  price {
                    amount
                    amountWithTaxesFees
                    amountWithInclusiveTax
                    amountPayableNow
                    amountPayAtProperty
                    amountWithFees
                    currencyCode
                    type
                    tax {
                        amount
                    }                                            
                  }
                  priceGroup {
                    amount
                    amountWithTaxesFees
                    amountWithInclusiveTax
                    amountPayableNow
                    amountPayAtProperty
                    amountWithFees
                    currencyCode
                    type
                    ref
                    refValue
                  }
                  hotel {
                    id
                  }
                }
                errorCode
                message
              }
              }
        """.trimIndent()
                log.debug("_____Graphql query prepared for rateCode_____")
                val jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
                log.info("converted query to json $jsonQuery")
                val calendarResponse = callLeadAvailability(jsonQuery)
                populateCalendarRes(calendarResponse)
            }

            (!req.promoCode.isNullOrEmpty() && !req.promoType.isNullOrEmpty()) -> {
                log.info("Lead availability with promo code request")
                val query = """
            query {
            getHotelLeadAvailability(leadAvailability: {
             hotelId:"${req.hotelId}",
             startDate:"${req.startDate}", 
             endDate:"${req.endDate}",
              numRooms: 1, 
              adults: 1, 
              children: 0, 
              onlyCheckRequested:"${Constants.ONLYCHECKREQUESTED}",
              lengthOfStay: ${req.lengthOfStay},
              promoCode: "${req.promoCode}",
              promoType: "${req.promoType}"
                }) 
              {
                leadAvailability {
                  leastRestrictiveFailure {
                    level
                    cause
                    overrideAllowed
                  }
                  failures {
                      cause
                      percentage
                  }
                  available
                  arrivalDate
                  departureDate
                  price {
                    amount
                    amountWithTaxesFees
                    amountWithInclusiveTax
                    amountPayableNow
                    amountPayAtProperty
                    amountWithFees
                    currencyCode
                    type
                    tax {
                        amount
                    }
                  }
                  priceGroup {
                    amount
                    amountWithTaxesFees
                    amountWithInclusiveTax
                    amountPayableNow
                    amountPayAtProperty
                    amountWithFees
                    currencyCode
                    type
                    ref
                    refValue
                  }
                  hotel {
                    id
                  }
                }
                errorCode
                message
              }
              }
        """.trimIndent()
                log.debug("_____Graphql query prepared for promocode_____")
                val jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
                log.info("converted query to json $jsonQuery")
                val calendarResponse = callLeadAvailability(jsonQuery)
                populateCalendarRes(calendarResponse)
            }

            (!req.agentId.isNullOrEmpty() && !req.agentType.isNullOrEmpty()) -> {
                log.info("Lead availability with rate agent id request")
                val query = """
            query {
            getHotelLeadAvailability(leadAvailability: {
             hotelId:"${req.hotelId}",
             startDate:"${req.startDate}", 
             endDate:"${req.endDate}",
              numRooms: 1, 
              adults: 1, 
              children: 0, 
              onlyCheckRequested:"${Constants.ONLYCHECKREQUESTED}",
              lengthOfStay: ${req.lengthOfStay},
              agentId: ${req.agentId.toInt()},
              agentType: "${req.agentType}"
                }) 
              {
                leadAvailability {
                  leastRestrictiveFailure {
                    level
                    cause
                    overrideAllowed
                  }
                  failures {
                      cause
                      percentage
                  }
                  available
                  arrivalDate
                  departureDate
                  price {
                    amount
                    amountWithTaxesFees
                    amountWithInclusiveTax
                    amountPayableNow
                    amountPayAtProperty
                    amountWithFees
                    currencyCode
                    type
                    tax {
                        amount
                    }
                  }
                  priceGroup {
                    amount
                    amountWithTaxesFees
                    amountWithInclusiveTax
                    amountPayableNow
                    amountPayAtProperty
                    amountWithFees
                    currencyCode
                    type
                    ref
                    refValue
                  }
                  hotel {
                    id
                  }
                }
                errorCode
                message
              }
              }
        """.trimIndent()
                log.debug("_____Graphql query prepared for agentId_____")
                val jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
                log.info("converted query to json $jsonQuery")
                val calendarResponse = callLeadAvailability(jsonQuery)
                populateCalendarRes(calendarResponse)
            }

            else -> {
                log.info("Lead availability with rateFilter request")
                val query = """
            query {
            getHotelLeadAvailability(leadAvailability: {
             hotelId:"${req.hotelId}",
             startDate:"${req.startDate}", 
             endDate:"${req.endDate}",
              numRooms: 1, 
              adults: 1, 
              children: 0, 
              onlyCheckRequested:"${Constants.ONLYCHECKREQUESTED}",
              lengthOfStay: ${req.lengthOfStay},
              rateFilter:"${req.rateFilter}"
                }) 
              {
                leadAvailability {
                  leastRestrictiveFailure {
                    level
                    cause
                    overrideAllowed
                  }
                  failures {
                      cause
                      percentage
                  }
                  available
                  arrivalDate
                  departureDate
                  price {
                    amount
                    amountWithTaxesFees
                    amountWithInclusiveTax
                    amountPayableNow
                    amountPayAtProperty
                    amountWithFees
                    currencyCode
                    type
                    tax {
                        amount
                    }
                  }
                  priceGroup {
                    amount
                    amountWithTaxesFees
                    amountWithInclusiveTax
                    amountPayableNow
                    amountPayAtProperty
                    amountWithFees
                    currencyCode
                    type
                    ref
                    refValue
                  }
                  hotel {
                    id
                  }
                }
                errorCode
                message
              }
              }
        """.trimIndent()
                log.debug("_____Graphql query prepared ratefilter_____")
                val jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
                log.info("converted query to json $jsonQuery")
                val calendarResponse = callLeadAvailability(jsonQuery)
                populateCalendarRes(calendarResponse)
            }
        }
    }

    private suspend fun callLeadAvailability(jsonBody: String): GetHotelLeadAvailabilityRes {
        log.info("callLeadAvailability request: ${jsonBody.json}")
        val token = generateToken.getToken()
        try {
            val response = ConfigureHTTPClient.client.post(prop.bookingDevUrl) {
                timeout {
                    requestTimeoutMillis = prop.requestTimeoutMillis.toLong()
                }
                headers {
                    append(Constants.AUTHORIZATION, token)
                }
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
            }
            if (response.status == HttpStatusCode.Forbidden) {
                generateToken.generateTokenAndSave()
                return callLeadAvailability(jsonBody)
            }
            log.debug("Response received from hudini api to get hotel lead  availability calender  is ${response.bodyAsText()}")
            return response.body() as GetHotelLeadAvailabilityRes
        } catch (e: Exception) {
            log.error("Exception occurred while calling api is ${e.message} due to ${e.stackTrace}")
            throw InternalServerException(e.message)
        }
    }
    private fun populateCalendarRes(response: GetHotelLeadAvailabilityRes): Any {
        log.info("manipulating calendar data")
        if(!response.data?.getHotelLeadAvailability?.leadAvailability?.get(0)?.price.isNullOrEmpty()) {
            if (response.data?.getHotelLeadAvailability?.leadAvailability?.get(0)?.price?.get(0)?.currencyCode != Constants.INR) {
                return populateNonIndianHotelsCalendarRes(response)
            } else {
                val leadAvailability = mutableListOf<LeadAvailability>()
                if (response.data.getHotelLeadAvailability.leadAvailability.isNotEmpty() == true) {
                    response.data.getHotelLeadAvailability.leadAvailability.forEach {
                        when {
                            ((it.leastRestrictiveFailure.cause.equals(Constants.No_SELL)) ||
                                    (it.leastRestrictiveFailure.cause.equals(Constants.CLOSED)) ||
                                    (it.leastRestrictiveFailure.cause.equals(Constants.SOLD_OUT)) ||
                                    (it.leastRestrictiveFailure.cause.equals(Constants.NO_AVAILABLE_INVENTORY)) ||
                                    (it.leastRestrictiveFailure.cause.equals(Constants.LIMIT)))
                                             -> {
                                it.available = false
                                leadAvailability.add(it)
                            }

                            else -> {
                                it.available = true
                                leadAvailability.add(it)
                            }
                        }
                    }
                }
                val getHotelLeadAvailability = GetHotelLeadAvailability(
                        leadAvailability,
                        errorCode = response.data.getHotelLeadAvailability.errorCode,
                        message = response.data.getHotelLeadAvailability.message
                )

                val data = Data(
                        getHotelLeadAvailability = getHotelLeadAvailability
                )
                return GetHotelLeadAvailabilityRes(
                        data = data,
                        errors = response.errors
                )
            }
        }else{
            return response
        }

    }
    private fun populateNonIndianHotelsCalendarRes(response: GetHotelLeadAvailabilityRes):NonIndainHotelsGetHotelLeadAvailabilityRes{
        log.info("non indian hotels calendar response mapping")
        val leadAvailability = mutableListOf<NonIndianHotelsLeadAvailability>()
        if (response.data?.getHotelLeadAvailability?.leadAvailability?.isNotEmpty() == true) {
            response.data.getHotelLeadAvailability.leadAvailability.forEach {
                when {
                    ((it.leastRestrictiveFailure.cause.equals(Constants.No_SELL)) ||
                            (it.leastRestrictiveFailure.cause.equals(Constants.CLOSED)) ||
                            (it.leastRestrictiveFailure.cause.equals(Constants.SOLD_OUT)) ||
                                    (it.leastRestrictiveFailure.cause.equals(Constants.NO_AVAILABLE_INVENTORY)) ||
                                    (it.leastRestrictiveFailure.cause.equals(Constants.LIMIT))) -> {
                        it.available = false
                        leadAvailability.add(nonIndianHotelsPriceDetails(it))
                    }
                    else -> {
                        it.available = true
                        leadAvailability.add(nonIndianHotelsPriceDetails(it))
                    }
                }
            }
        }
        val getHotelLeadAvailability = NonIndianHotelsGetHotelLeadAvailability(
            leadAvailability,
            errorCode = response.data?.getHotelLeadAvailability?.errorCode,
            message = response.data?.getHotelLeadAvailability?.message
        )
        val data = NonIndianHotelsData(
            getHotelLeadAvailability = getHotelLeadAvailability
        )
        return NonIndainHotelsGetHotelLeadAvailabilityRes(
            data = data,
            errors = response.errors
        )
    }
    private fun nonIndianHotelsPriceDetails(it: LeadAvailability): NonIndianHotelsLeadAvailability {
        log.info("non indian hotels price and price group response mapping")
        val price = mutableListOf<NonIndianHotelsPrice>()
        val priceGroup = mutableListOf<NonIndianHotelsPriceGroup>()
        it.price?.forEach {
            price.add(
                NonIndianHotelsPrice(
                    amount = it?.amount?.toInt(),
                    amountPayAtProperty = it?.amountPayAtProperty,
                    amountPayableNow = it?.amountPayableNow,
                    amountWithInclusiveTax = it?.amountWithInclusiveTax?.toInt(),
                    amountWithTaxesFees = it?.amountWithTaxesFees?.toInt(),
                    amountWithFees = it?.amountWithFees,
                    currencyCode = it?.currencyCode,
                    type = it?.type,
                    tax = it?.tax
                )
            )
        }
        it.priceGroup?.forEach {
            priceGroup.add(
                NonIndianHotelsPriceGroup(
                    amount = it?.amount?.toInt(),
                    amountPayAtProperty = it?.amountPayAtProperty,
                    amountPayableNow = it?.amountPayableNow,
                    amountWithInclusiveTax = it?.amountWithInclusiveTax?.toInt(),
                    amountWithTaxesFees = it?.amountWithTaxesFees?.toInt(),
                    currencyCode = it?.currencyCode,
                    ref = it?.ref,
                    refValue = it?.refValue,
                    type = it?.type
                )
            )
        }
        return NonIndianHotelsLeadAvailability(
            arrivalDate = it.arrivalDate,
            available = it.available,
            departureDate = it.departureDate,
            hotel = it.hotel,
            price = price,
            priceGroup = priceGroup,
            leastRestrictiveFailure = it.leastRestrictiveFailure,
            failures = it.failures
        )
    }
}