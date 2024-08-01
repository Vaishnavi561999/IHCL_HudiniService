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
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.koin.java.KoinJavaComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.ktor.client.request.headers

class DestinationAvailabilityService {
    private val createBooking by KoinJavaComponent.inject<CreateBookingService>(CreateBookingService::class.java)
    private val generateToken by KoinJavaComponent.inject<GenerateToken>(GenerateToken::class.java)
    val log:Logger = LoggerFactory.getLogger(javaClass)
    private val prop = PropertiesConfiguration.env

    suspend fun checkDestinationAvailability(request: GetHotelLeadAvailabilityCalenderReq): GetHotelLeadAvailabilityRes {
        if(request.hotelIds.isNullOrEmpty() || request.startDate.isNullOrEmpty() || request.endDate.isNullOrEmpty()) {
            throw HttpResponseException("Mandatory fields are missing!!", HttpStatusCode.BadRequest)
        }
        validateDateFormat(request.startDate,request.endDate)
        log.info("checkDestinationAvailability request: ${request.hotelIds}")
        val token = generateToken.getToken()
        val hotelIds = buildString {
            append("[")
            append(request.hotelIds.joinToString(",") { "\"$it\"" })
            append("]")
        }
        val query = """query {
                        getHotelLeadAvailability(leadAvailability: {
                        hotelIds: $hotelIds,
                        startDate: "${request.startDate}", 
                        endDate: "${request.endDate}",
                        numRooms: ${request.numRooms}, 
                        adults: ${request.adults}, 
                        children: ${request.children}, 
                        onlyCheckRequested: "${Constants.ONLYCHECKREQUESTED}",
                        rateFilter: ["${request.rateFilter}"]
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
                            hudiniId
                          }
                        }
                        errorCode
                        message
                      }
                      }
                    """.trimIndent()
        log.debug("_____Destinations API Graphql query prepared as_____")
        val jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
        log.debug("converted query to json $jsonQuery")
        try {
            val response = ConfigureHTTPClient.client.post(prop.bookingDevUrl) {
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
                return checkDestinationAvailability(request)
            }
            log.debug("Response received from hudini destinations api to get hotel lead availability calender is ${response.bodyAsText()}")
            val hudiniResponse = response.body<GetHotelLeadAvailabilityRes>()
            return populateDestinationRates(hudiniResponse)
        } catch (e: Exception) {
            log.error("Exception occurred while calling api is ${e.message} due to ${e.stackTrace}")
            throw InternalServerException(e.message)
        }
    }
    private fun populateDestinationRates(hudiniResponse:GetHotelLeadAvailabilityRes?):GetHotelLeadAvailabilityRes{
        log.info("manipulating destinations data")
        val leadAvailability = mutableListOf<LeadAvailability>()
        if(hudiniResponse?.data?.getHotelLeadAvailability?.leadAvailability?.isNotEmpty() == true) {
            hudiniResponse.data.getHotelLeadAvailability.leadAvailability.forEach {
                when {
                    ((it.leastRestrictiveFailure.cause.equals(Constants.No_SELL)) ||
                            (it.leastRestrictiveFailure.cause.equals(Constants.CLOSED)) ||
                            (it.leastRestrictiveFailure.cause.equals(Constants.SOLD_OUT)) ||
                            (it.leastRestrictiveFailure.cause.equals(Constants.NO_AVAILABLE_INVENTORY)) ||
                            (it.leastRestrictiveFailure.cause.equals(Constants.LIMIT))) -> {
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
            errorCode = hudiniResponse?.data?.getHotelLeadAvailability?.errorCode,
            message = hudiniResponse?.data?.getHotelLeadAvailability?.message
        )

        val data = Data(
            getHotelLeadAvailability = getHotelLeadAvailability
        )

        return GetHotelLeadAvailabilityRes(
            data = data,
            errors = hudiniResponse?.errors
        )
    }
}