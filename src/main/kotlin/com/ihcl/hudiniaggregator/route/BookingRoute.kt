package com.ihcl.hudiniaggregator.route

import com.ihcl.hudiniaggregator.dto.*
import com.ihcl.hudiniaggregator.dto.checkAvailability.ChangeDatesCheckAvailabilityRequest
import com.ihcl.hudiniaggregator.dto.checkAvailability.ChangeDatesCheckAvailabilityResponse
import com.ihcl.hudiniaggregator.dto.checkAvailability.CheckAvailabilityInput
import com.ihcl.hudiniaggregator.dto.createBooking.CreateBookingRequest
import com.ihcl.hudiniaggregator.dto.createBooking.CreateBookingResponse
import com.ihcl.hudiniaggregator.dto.getBookingDetails.BookingDetailsRequest
import com.ihcl.hudiniaggregator.dto.getBookingDetails.BookingDetailsResponse
import com.ihcl.hudiniaggregator.dto.updateBooking.UpdateBookingRequest
import com.ihcl.hudiniaggregator.dto.updateBooking.UpdateBookingResponse
import com.ihcl.hudiniaggregator.service.*
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.java.KoinJavaComponent
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun Application.hudiniRoute() {
    val log: Logger = LoggerFactory.getLogger(javaClass)
    val sebService by KoinJavaComponent.inject<SEBService>(SEBService::class.java)
    val checkAvailabilityService by KoinJavaComponent.inject<CheckAvailabilityService>(CheckAvailabilityService::class.java)
    val rateCodePromoCodeService by KoinJavaComponent.inject<RateCodePromoCodeService>(RateCodePromoCodeService::class.java)
    val bookingDetailsService by KoinJavaComponent.inject<BookingDetailsService>(BookingDetailsService::class.java)
    val createBookingService by KoinJavaComponent.inject<CreateBookingService>(CreateBookingService::class.java)
    val cancelBookingService by KoinJavaComponent.inject<CancelBookingService>(CancelBookingService::class.java)
    val updateBookingService by KoinJavaComponent.inject<UpdateBookingService>(UpdateBookingService::class.java)
    val rateCodeCheckAvailabilityService by KoinJavaComponent.inject<RateCodeCheckAvailabilityService>(RateCodeCheckAvailabilityService::class.java)
    val getHotelLeadAvailabilityCalenderService by KoinJavaComponent.inject<GetHotelLeadAvailabilityCalenderService>(
        GetHotelLeadAvailabilityCalenderService::class.java
    )
    val destinationAvailabilityService by KoinJavaComponent.inject<DestinationAvailabilityService>(
        DestinationAvailabilityService::class.java
    )
    routing {
        get("/") {
            call.respond("Hudini Service......")
        }
        route("/v1") {
            post("/hotel-availability") {
                log.info("hotel-availability route")
                val request: CheckAvailabilityInput = call.receive()
                val res = checkAvailabilityService.checkHotelAvailability(request)
                call.respond(res as Any)
            }
            post("/fetch-booking-details") {
                log.info("fetch-booking-details route")
                val request: BookingDetailsRequest = call.receive()
                val res = bookingDetailsService.getBookingDetails(request)
                call.respond(res.body() as BookingDetailsResponse)
            }
            post("/create-booking") {
                log.info("create-booking route")
                val request: CreateBookingRequest = call.receive()
                val res = createBookingService.createBooking(request)
                call.respond(res.body() as CreateBookingResponse)
            }
            post("/cancel-booking") {
                log.info("cancel-booking route")
                val request: CancelBookingInput = call.receive()
                val res = cancelBookingService.cancelBooking(request)
                when (res.status.value) {
                    HttpStatusCode.OK.value -> {
                        call.respond(res.body() as CancelBookingResponse)
                    }

                    HttpStatusCode.NotFound.value -> {
                        call.respond(res.body() as HudiniErrorResponse)
                    }

                    else -> {
                        call.respond(res.status.value, res.body())
                    }
                }
            }
            post("/update-booking") {
                log.info("update-booking route")
                val request: UpdateBookingRequest = call.receive()
                val res = updateBookingService.updateBookingDetails(request)
                call.respond(res.body() as UpdateBookingResponse)
            }
            post("/calendar-view") {
                log.info("calendar-view route")
                val request: GetHotelLeadAvailabilityCalenderReq = call.receive()
                val res = getHotelLeadAvailabilityCalenderService.getHotelLeadAvailabilityCalender(request)
                call.respond(res)
            }
            post("/rate-promo-availability") {
                log.info("rate-promo-availability route")
                val request = call.receive<CheckAvailabilityInput>()
                val res = rateCodePromoCodeService.getRateCodeOrPromoCode(request)
                call.respond(res!!)
            }
            post("/destination-availability") {
                log.info("destination-availability route")
                val request = call.receive<GetHotelLeadAvailabilityCalenderReq>()
                call.respond(destinationAvailabilityService.checkDestinationAvailability(request))
            }
            post("/change-dates-availability") {
                log.info("change-dates-availability route")
                val request = call.receive<ChangeDatesCheckAvailabilityRequest>()
                val response = rateCodeCheckAvailabilityService.checkRateCodeAvailability(request)
                call.respond(response as ChangeDatesCheckAvailabilityResponse)
            }
            post("/seb-bookings") {
                log.info("seb-bookings route")
                val request = call.receive<SEBRequest>()
                val response = sebService.updateEmployeeDetails(request)
                call.respond(response)
            }
        }
    }
}