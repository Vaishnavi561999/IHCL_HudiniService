package com.ihcl.hudiniaggregator.service

import com.ihcl.hudiniaggregator.config.PropertiesConfiguration
import com.ihcl.hudiniaggregator.dto.checkAvailability.*
import com.ihcl.hudiniaggregator.dto.hotelAvailability.*
import com.ihcl.hudiniaggregator.dto.hotelAvailability.RoomTypes
import com.ihcl.hudiniaggregator.dto.hotelAvailability.Rooms
import com.ihcl.hudiniaggregator.dto.hotelAvailability.StandardRate
import com.ihcl.hudiniaggregator.exceptions.InternalServerException
import com.ihcl.hudiniaggregator.plugins.ConfigureHTTPClient
import io.ktor.client.plugins.timeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.ihcl.hudiniaggregator.util.Constants
import com.ihcl.hudiniaggregator.util.GenerateToken
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.future.future
import org.koin.java.KoinJavaComponent
import org.litote.kmongo.json
import java.util.concurrent.CompletableFuture

class CheckAvailabilityService {
    private val prop = PropertiesConfiguration.env
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val createBooking by KoinJavaComponent.inject<CreateBookingService>(CreateBookingService::class.java)
    private val generateToken by KoinJavaComponent.inject<GenerateToken>(GenerateToken::class.java)

    suspend fun checkHotelAvailability(request: CheckAvailabilityInput): Any? {
        val result: Any?
        log.info("check hotel availability request hotelId :${request.hotelId} start date: ${request.startDate} end date ${request.endDate}")
        when {
            //check availability
            (request.isMemberOffer1 == true) || (request.isMemberOffer2 == true) || (request.isOfferLandingPage == false) && (request.isMyAccount == false) && (request.isCorporate == false) && (request.agentId == null) && (request.promoCode == null) && (request.couponCode == null) && (request.rateCode == null) -> {
                result = getStandardMemberPackageAndMemberDealRates(request)
            }
            //promo + coupon
            (!request.promoCode.isNullOrEmpty()) && (!request.couponCode.isNullOrEmpty()) -> {
                result = getPromoCodeCouponCodeRates(request)
            }
            //check availability + offers || promotions
            ((request.rateCode != null) && (request.isOfferLandingPage == false) && (request.isMyAccount == false) && (request.isCorporate == false) && (request.isEmployeeOffer == false) ) || ((request.promoCode != null) && (request.isOfferLandingPage == false) && (request.isMyAccount == false) && (request.isCorporate == true) && (request.isEmployeeOffer == false)) -> {
                result = getRates(request)
            }
            //rateCode || promoCode || agentCode || couponCode || SEB & FnF
            else -> {
                result =
                    if ((request.isOfferLandingPage == true) && (request.isMyAccount == false) && (request.isCorporate == false) && (request.isEmployeeOffer == false)) {
                        getRates(request)
                    } else {
                        val graphQlQuery = prepareGraphQlQuery(request, null)
                        val offerOrPromotionResponse = callCheckHotelAvailability(graphQlQuery)
                        getOffersOrPromotionRates(request, offerOrPromotionResponse, null)
                    }
            }
        }
        return result
    }

    //calling hudini check availability API
    private suspend fun callCheckHotelAvailability(jsonQuery: String): CheckAvailabilityResponse {
        log.debug("CheckHotelAvailability request $jsonQuery")
        val token = generateToken.getToken()
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
            val res = response.body<CheckAvailabilityResponse>()
            if (response.status == HttpStatusCode.Forbidden) {
                generateToken.generateTokenAndSave()
                return callCheckHotelAvailability(jsonQuery)
            }
            log.debug(
                "Response received from hudini check availability {}",
                response.bodyAsText()
            )
            return res
        } catch (e: Exception) {
            log.error("Exception occurred while calling hotel availability api is ${e.message} due to ${e.stackTrace}")
            throw InternalServerException(e.message)
        }
    }

    //mapping std, member, packages and member deal rates
    private suspend fun getStandardMemberPackageAndMemberDealRates(request: CheckAvailabilityInput): HotelAvailabilityResponse? {
        log.info("getStandardMemberPackageAndMemberDealRates request ${request.hotelId}, rate filter: ${request.rateFilter}")
        var result: HotelAvailabilityResponse?
        val rateFilter = request.rateFilter?.split(",")
        val rateFilterAndPackageQuery = prepareGraphQlQuery(request, rateFilter?.get(1))
        val memberDealsQuery = prepareGraphQlQuery(request, rateFilter?.get(2))
        val rateFilterAndPackages = CompletableFuture.supplyAsync {
            kotlinx.coroutines.runBlocking {
                future { callCheckHotelAvailability(rateFilterAndPackageQuery) }
            }
        }
        val memberDeals = CompletableFuture.supplyAsync {
            kotlinx.coroutines.runBlocking {
                future { callCheckHotelAvailability(memberDealsQuery) }
            }
        }
        result = mapRateFilterRates(request, rateFilterAndPackages.get().get())
        result = mapPackagesRates(request, result, rateFilterAndPackages.get().get())
        result = mapMemberDealRates(request, result, memberDeals.get().get())
        result?.tab?.clear()
        result?.tab?.add(Constants.ROOM_RATES)
        if (!result?.roomAvailability?.memberExclusiveRates.isNullOrEmpty()) {
            result?.tab?.add(Constants.MEMBER_DEAL)
        }
        if (!result?.roomAvailability?.packagesRates.isNullOrEmpty()) {
            result?.tab?.add(Constants.PACKAGES)
        }
        return result
    }

    //mapping common rates from promocode and couponcode response
    private suspend fun getPromoCodeCouponCodeRates(request: CheckAvailabilityInput): HotelAvailabilityResponse? {
        log.info("getPromoCodeCouponCodeRates request: ${request.hotelId}, rate filter: ${request.rateFilter}")
        val promoCodeQuery = prepareGraphQlQuery(request, Constants.PROMOCODE)
        val couponCodeQuery = prepareGraphQlQuery(request, Constants.COUPONCODE)
        val promoCodeResponse = callCheckHotelAvailability(promoCodeQuery)
        val couponCodeResponse = callCheckHotelAvailability(couponCodeQuery)
        val promotionRates: MutableList<PackageRoomTypes?> = mutableListOf()

        val couponCodeHotelAvailabilityRes = CompletableFuture.supplyAsync {
            kotlinx.coroutines.runBlocking {
                future { getOffersOrPromotionRates(request, couponCodeResponse, Constants.COUPONCODE) }
            }
        }
        val promoCodeHotelAvailabilityRes = CompletableFuture.supplyAsync {
            kotlinx.coroutines.runBlocking {
                future { getOffersOrPromotionRates(request, promoCodeResponse, Constants.PROMOCODE) }
            }
        }

        val couponCodeHotelAvailability = couponCodeHotelAvailabilityRes.get().get()
        val promoCodeHotelAvailability = promoCodeHotelAvailabilityRes.get().get()

        return mapHotelAvailabilityResponse(couponCodeHotelAvailability, promoCodeHotelAvailability, promotionRates)
    }

    private fun mapHotelAvailabilityResponse(
        couponCodeHotelAvailability: HotelAvailabilityResponse?,
        promoCodeHotelAvailability: HotelAvailabilityResponse?,
        promotionRates: MutableList<PackageRoomTypes?>,
    ): HotelAvailabilityResponse? {
        log.info("mapHotelAvailabilityResponse hotelId: ${couponCodeHotelAvailability?.hotelId} ")
        val result: HotelAvailabilityResponse
        promoCodeHotelAvailability?.roomAvailability?.promotionRates?.forEach { promoRooms ->
            couponCodeHotelAvailability?.roomAvailability?.promotionRates?.forEach { couponRooms ->
                if (promoRooms?.roomCode == couponRooms?.roomCode) {
                    promoRooms?.rooms?.forEach { promoRates ->
                        couponRooms?.rooms?.forEach { couponRates ->
                            if (promoRates?.rateCode == couponRates?.rateCode) {
                                val rooms: MutableList<PackageRooms?> = mutableListOf()
                                rooms.add(couponRates)
                                val packageRoomTypes = PackageRoomTypes(
                                    roomCode = couponRooms.roomCode,
                                    rooms = rooms
                                )
                                promotionRates.add(packageRoomTypes)
                            }
                        }
                    }
                }
            }
        }

        if (promotionRates.isEmpty()) {
            result = HotelAvailabilityResponse(
                tab = couponCodeHotelAvailability?.tab,
                hotelId = couponCodeHotelAvailability?.hotelId,
                synxisId = couponCodeHotelAvailability?.synxisId,
                couponCode = couponCodeHotelAvailability?.couponCode,
                isCouponCodeValid = false,
                couponRemark = Constants.NO_COMMON_ROOMS,
                roomAvailability = Rates(
                    roomRates = mutableListOf(),
                    packagesRates = mutableListOf(),
                    memberExclusiveRates = mutableListOf(),
                    offerRates = mutableListOf(),
                    promotionRates = promotionRates
                ),
                chargeList = couponCodeHotelAvailability?.chargeList,
                leastRestrictiveFailure = couponCodeHotelAvailability?.leastRestrictiveFailure,
                errorCode = couponCodeHotelAvailability?.errorCode,
                message = couponCodeHotelAvailability?.message
            )
            return sortRates(result)
        }
        result = HotelAvailabilityResponse(
            tab = couponCodeHotelAvailability?.tab,
            hotelId = couponCodeHotelAvailability?.hotelId,
            synxisId = couponCodeHotelAvailability?.synxisId,
            couponCode = couponCodeHotelAvailability?.couponCode,
            isCouponCodeValid = couponCodeHotelAvailability?.isCouponCodeValid,
            couponRemark = couponCodeHotelAvailability?.couponRemark,
            roomAvailability = Rates(
                roomRates = mutableListOf(),
                packagesRates = mutableListOf(),
                memberExclusiveRates = mutableListOf(),
                offerRates = mutableListOf(),
                promotionRates = promotionRates
            ),
            chargeList = couponCodeHotelAvailability?.chargeList,
            leastRestrictiveFailure = couponCodeHotelAvailability?.leastRestrictiveFailure,
            errorCode = couponCodeHotelAvailability?.errorCode,
            message = couponCodeHotelAvailability?.message
        )

        return sortRates(result)
    }

    private fun sortRates(result: HotelAvailabilityResponse?): HotelAvailabilityResponse? {
        log.info("sortRates hotelId: ${result?.hotelId}")
        sortRoomRates(result)
        sortPackageRates(result)
        sortOfferRates(result)
        sortPromotionRates(result)
        sortMemberExclusiveRates(result)
        return result
    }

    private fun sortRoomRates(result: HotelAvailabilityResponse?) {
        log.info("sortRoomRates hotel id: ${result?.hotelId}")
        //sorting rates under individual rooms based on member rates
        if (!result?.roomAvailability?.roomRates.isNullOrEmpty()) {
            result?.roomAvailability?.roomRates?.forEach { roomRate ->
                if (!roomRate?.rooms.isNullOrEmpty()) {
                    roomRate?.rooms = roomRate?.rooms
                        ?.sortedBy { room ->
                            room?.memberRate?.total?.amount
                        }?.toMutableList()

                    val nullMemberRateRooms = roomRate?.rooms
                        ?.filter { room ->
                            room?.memberRate == null
                        }

                    roomRate?.rooms?.removeAll(nullMemberRateRooms ?: emptyList())
                    roomRate?.rooms?.addAll(nullMemberRateRooms ?: emptyList())
                }
            }
        }
        //sorting rooms based on member rates
        if (!result?.roomAvailability?.roomRates.isNullOrEmpty()) {
            val filteredRoomRates = result?.roomAvailability?.roomRates
                ?.filter { roomRate ->
                    roomRate?.rooms?.isNotEmpty() == true &&
                            roomRate.rooms?.get(0)?.memberRate?.total?.amount != null
                }?.toMutableList()

            val nullAmountRoomRates = result?.roomAvailability?.roomRates
                ?.filter { roomRate ->
                    roomRate?.rooms?.isNotEmpty() == true &&
                            roomRate.rooms?.get(0)?.memberRate?.total?.amount == null
                }

            result?.roomAvailability?.roomRates = filteredRoomRates
                ?.sortedBy { roomRate ->
                    roomRate?.rooms?.get(0)?.memberRate?.total?.amount
                }?.toMutableList()

            result?.roomAvailability?.roomRates?.addAll(nullAmountRoomRates ?: listOf())
        }
    }

    private fun sortPackageRates(result: HotelAvailabilityResponse?) {
        log.info("sortPackageRates result: ${result?.hotelId}")
        //sorting package rates under individual room
        if (!result?.roomAvailability?.packagesRates.isNullOrEmpty()) {
            result?.roomAvailability?.packagesRates?.forEach {
                if (!it?.rooms.isNullOrEmpty()) {
                    it?.rooms = it?.rooms?.sortedBy { room ->
                        room?.total?.amount
                    }?.toMutableList()
                }
            }
        }
        //sorting rooms
        if (!result?.roomAvailability?.packagesRates.isNullOrEmpty()) {
            result?.roomAvailability?.packagesRates = result?.roomAvailability?.packagesRates
                ?.filter { packageRate ->
                    packageRate?.rooms?.isNotEmpty() == true &&
                            packageRate.rooms?.get(0)?.total?.amount != null
                }
                ?.sortedBy { packageRate ->
                    packageRate?.rooms?.get(0)?.total?.amount
                }?.toMutableList()
        }
    }

    private fun sortPromotionRates(result: HotelAvailabilityResponse?) {
        log.info("sortPromotionRates result: ${result?.hotelId}")
        //sorting promotion rates
        if (!result?.roomAvailability?.promotionRates.isNullOrEmpty()) {
            result?.roomAvailability?.promotionRates?.forEach {
                if (!it?.rooms.isNullOrEmpty()) {
                    it?.rooms = it?.rooms?.sortedBy { room ->
                        room?.total?.amount
                    }?.toMutableList()
                }
            }
        }
        //sorting rooms
        if (!result?.roomAvailability?.promotionRates.isNullOrEmpty()) {
            result?.roomAvailability?.promotionRates = result?.roomAvailability?.promotionRates
                ?.filter { promotionRate ->
                    promotionRate?.rooms?.isNotEmpty() == true &&
                            promotionRate.rooms?.get(0)?.total?.amount != null
                }
                ?.sortedBy { promotionRate ->
                    promotionRate?.rooms?.get(0)?.total?.amount
                }?.toMutableList()
        }
    }

    private fun sortMemberExclusiveRates(result: HotelAvailabilityResponse?) {
        log.info("sortMemberExclusiveRates hotel id: ${result?.hotelId}")
        //sorting member exclusive rates
        if (!result?.roomAvailability?.memberExclusiveRates.isNullOrEmpty()) {
            result?.roomAvailability?.memberExclusiveRates?.forEach {
                if (!it?.rooms.isNullOrEmpty()) {
                    it?.rooms = it?.rooms?.sortedBy { room ->
                        room?.total?.amount
                    }?.toMutableList()
                }
            }
        }
        //sorting rooms
        if (!result?.roomAvailability?.memberExclusiveRates.isNullOrEmpty()) {
            result?.roomAvailability?.memberExclusiveRates = result?.roomAvailability?.memberExclusiveRates
                ?.filter { memberExclusiveRate ->
                    memberExclusiveRate?.rooms?.isNotEmpty() == true &&
                            memberExclusiveRate.rooms?.get(0)?.total?.amount != null
                }
                ?.sortedBy { memberExclusiveRate ->
                    memberExclusiveRate?.rooms?.get(0)?.total?.amount
                }?.toMutableList()
        }
    }

    private fun sortOfferRates(result: HotelAvailabilityResponse?) {
        log.info("sortOfferRates hotelId: ${result?.hotelId}")
        //sorting offer rates
        if (!result?.roomAvailability?.offerRates.isNullOrEmpty()) {
            result?.roomAvailability?.offerRates?.forEach {
                if (!it?.rooms.isNullOrEmpty()) {
                    it?.rooms = it?.rooms?.sortedBy { room ->
                        room?.total?.amount
                    }?.toMutableList()
                }
            }
        }
        //sorting rooms
        if (!result?.roomAvailability?.offerRates.isNullOrEmpty()) {
            result?.roomAvailability?.offerRates = result?.roomAvailability?.offerRates
                ?.filter { offersRate ->
                    offersRate?.rooms?.isNotEmpty() == true &&
                            offersRate.rooms?.get(0)?.total?.amount != null
                }
                ?.sortedBy { offersRate ->
                    offersRate?.rooms?.get(0)?.total?.amount
                }?.toMutableList()
        }
    }

    // preparing check availability response for offers or promotions
    private fun getOffersOrPromotionRates(
        request: CheckAvailabilityInput,
        response: CheckAvailabilityResponse,
        specialCode: String?,
    ): HotelAvailabilityResponse? {
        log.info("getOffersOrPromotionRates hotelId: ${request.hotelId}")
        var hotelAvailabilityResponse: HotelAvailabilityResponse? = null
        val tab: MutableSet<String?> = mutableSetOf(Constants.PROMOTIONS)
        if (request.rateCode != null) {
            tab.clear()
            tab.add(Constants.OFFERS)
        }

        when {
            (request.rateCode != null) -> {
                hotelAvailabilityResponse = HotelAvailabilityResponse(
                    tab = tab,
                    hotelId = request.hotelId,
                    synxisId = response.data?.getHotelAvailability?.roomAvailability?.hotel?.id,
                    couponCode = null,
                    isCouponCodeValid = false,
                    couponRemark = null,
                    roomAvailability = Rates(
                        roomRates = mutableListOf(),
                        packagesRates = mutableListOf(),
                        memberExclusiveRates = mutableListOf(),
                        offerRates = getOffersAndPromotionDetails(request, response, null),
                        promotionRates = mutableListOf()
                    ),
                    chargeList = response.data?.getHotelAvailability?.contentLists?.chargeList,
                    leastRestrictiveFailure = response.data?.getHotelAvailability?.roomAvailability?.leastRestrictiveFailure,
                    errorCode = response.data?.getHotelAvailability?.errorCode,
                    message = response.data?.getHotelAvailability?.message
                )
            }

            (specialCode == Constants.COUPONCODE || !request.couponCode.isNullOrEmpty() && request.promoCode.isNullOrEmpty() && request.promoType.isNullOrEmpty() && request.agentId.isNullOrEmpty() && request.agentType.isNullOrEmpty()) -> {
                val res: HotelAvailabilityResponse?
                if (response.data?.getHotelAvailability?.warning != null) {
                    res = HotelAvailabilityResponse(
                        tab = tab,
                        hotelId = request.hotelId,
                        synxisId = response.data.getHotelAvailability.roomAvailability?.hotel?.id,
                        couponCode = request.couponCode,
                        isCouponCodeValid = false,
                        couponRemark = Constants.INVALID_COUPON_CODE,
                        roomAvailability = Rates(
                            roomRates = mutableListOf(),
                            packagesRates = mutableListOf(),
                            memberExclusiveRates = mutableListOf(),
                            offerRates = mutableListOf(),
                            promotionRates = getOffersAndPromotionDetails(request, response, Constants.COUPONCODE)
                        ),
                        chargeList = response.data.getHotelAvailability.contentLists?.chargeList,
                        leastRestrictiveFailure = response.data.getHotelAvailability.roomAvailability?.leastRestrictiveFailure,
                        errorCode = response.data.getHotelAvailability.errorCode,
                        message = response.data.getHotelAvailability.message
                    )
                } else {
                    res = HotelAvailabilityResponse(
                        tab = tab,
                        hotelId = request.hotelId,
                        synxisId = response.data?.getHotelAvailability?.roomAvailability?.hotel?.id,
                        couponCode = request.couponCode,
                        isCouponCodeValid = true,
                        couponRemark = Constants.COUPON_CODE_APPLIED_SUCCESSFULLY,
                        roomAvailability = Rates(
                            roomRates = mutableListOf(),
                            packagesRates = mutableListOf(),
                            memberExclusiveRates = mutableListOf(),
                            offerRates = mutableListOf(),
                            promotionRates = getOffersAndPromotionDetails(request, response, Constants.COUPONCODE)
                        ),
                        chargeList = response.data?.getHotelAvailability?.contentLists?.chargeList,
                        leastRestrictiveFailure = response.data?.getHotelAvailability?.roomAvailability?.leastRestrictiveFailure,
                        errorCode = response.data?.getHotelAvailability?.errorCode,
                        message = response.data?.getHotelAvailability?.message
                    )
                    if (res.roomAvailability?.promotionRates?.isEmpty() == true) {
                        res.isCouponCodeValid = false
                        res.couponRemark = Constants.USED_VALID_COUPON
                    }
                }
                return sortRates(res)
            }

            (specialCode == Constants.PROMOCODE || request.promoCode != null && request.promoType != null && request.agentId.isNullOrEmpty() && request.agentType.isNullOrEmpty() && request.couponCode.isNullOrEmpty()) -> {
                hotelAvailabilityResponse = HotelAvailabilityResponse(
                    tab = tab,
                    hotelId = request.hotelId,
                    synxisId = response.data?.getHotelAvailability?.roomAvailability?.hotel?.id,
                    couponCode = null,
                    isCouponCodeValid = false,
                    couponRemark = null,
                    roomAvailability = Rates(
                        roomRates = mutableListOf(),
                        packagesRates = mutableListOf(),
                        memberExclusiveRates = mutableListOf(),
                        offerRates = mutableListOf(),
                        promotionRates = getOffersAndPromotionDetails(request, response, Constants.PROMOCODE)
                    ),
                    chargeList = response.data?.getHotelAvailability?.contentLists?.chargeList,
                    leastRestrictiveFailure = response.data?.getHotelAvailability?.roomAvailability?.leastRestrictiveFailure,
                    errorCode = response.data?.getHotelAvailability?.errorCode,
                    message = response.data?.getHotelAvailability?.message,

                    )
            }

            (request.agentId != null && request.agentType != null) -> {
                hotelAvailabilityResponse = HotelAvailabilityResponse(
                    tab = tab,
                    hotelId = request.hotelId,
                    synxisId = response.data?.getHotelAvailability?.roomAvailability?.hotel?.id,
                    couponCode = null,
                    isCouponCodeValid = false,
                    couponRemark = null,
                    roomAvailability = Rates(
                        roomRates = mutableListOf(),
                        packagesRates = mutableListOf(),
                        memberExclusiveRates = mutableListOf(),
                        offerRates = mutableListOf(),
                        promotionRates = getOffersAndPromotionDetails(request, response, null)
                    ),
                    chargeList = response.data?.getHotelAvailability?.contentLists?.chargeList,
                    leastRestrictiveFailure = response.data?.getHotelAvailability?.roomAvailability?.leastRestrictiveFailure,
                    errorCode = response.data?.getHotelAvailability?.errorCode,
                    message = response.data?.getHotelAvailability?.message
                )
            }
        }
        return sortRates(hotelAvailabilityResponse)
    }

    //returns offers and promotion rates
    private fun getOffersAndPromotionDetails(
        request: CheckAvailabilityInput,
        response: CheckAvailabilityResponse,
        specialCode: String?,
    ): MutableList<PackageRoomTypes?> {
        log.info("getOffersAndPromotionDetails hotelId: ${request.hotelId}")
        val rateFilter = request.rateFilter?.split(",")
        val rateFilterRoomCodes = mutableSetOf<String>()
        val memberRateCode = mutableSetOf<String>()
        val mappedRateCodes = mutableSetOf<String?>()
        response.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rates ->
            rateFilterRoomCodes.add(rates.product?.room?.code.toString())
            memberRateCode.add(
                rates.product?.rate?.code!!
            )
        }
        response.data?.getHotelAvailability?.rateFilterMap?.forEach { rfm ->
            if (rfm?.name == rateFilter?.get(0)) {
                rfm?.rateCodeMapping?.forEach {
                    mappedRateCodes.add(it.key)
                    mappedRateCodes.add(it.value)
                }
            }
        }
        log.debug("Total room codes in promotion or offer rates{}", rateFilterRoomCodes)
        log.debug("Total rate codes in promotion or offer rates {}", memberRateCode)
        log.debug("Mapped rate code {}", mappedRateCodes)
        return mapOffersPromotionsRoomDetails(specialCode, request, response, rateFilterRoomCodes)
    }

    private fun mapOffersPromotionsRoomDetails(
        specialCode: String?,
        request: CheckAvailabilityInput,
        response: CheckAvailabilityResponse,
        rateFilterRoomCodes: MutableSet<String>,
    ): MutableList<PackageRoomTypes?> {
        log.info("mapOffersPromotionsRoomDetails hotelId: ${request.hotelId}")
        val rooms: MutableList<PackageRoomTypes?> = mutableListOf()
        //mapping room content and rooms to particular room code
        //creating room type objects
        for (i in 0 until rateFilterRoomCodes.size) {
            rooms.add(
                PackageRoomTypes(
                    roomCode = rateFilterRoomCodes.toList()[i],
                    rooms = mutableListOf(),
                )
            )
        }
        when {
            (specialCode == Constants.COUPONCODE || request.couponCode != null && request.rateCode.isNullOrEmpty() && request.promoCode.isNullOrEmpty() && request.promoType.isNullOrEmpty() && request.agentId.isNullOrEmpty() && request.agentType.isNullOrEmpty()) -> {
                mapOffersPromotionsRoomContent(rooms, response, request)
            }

            (!request.agentId.isNullOrEmpty() && !request.agentType.isNullOrEmpty()) -> {
                mapOffersPromotionsRoomContent(rooms, response, request)
            }

            (!request.rateCode.isNullOrEmpty()) -> {
                mapOffersPromotionsRoomContent(rooms, response, request)
            }

            (specialCode == Constants.PROMOCODE || !request.promoCode.isNullOrEmpty() && request.rateCode.isNullOrEmpty() && request.agentId.isNullOrEmpty() && request.agentType.isNullOrEmpty() && request.couponCode.isNullOrEmpty()) -> {
                mapOffersPromotionsRoomContent(rooms, response, request)
            }

            else -> {
                throw InternalServerException("Invalid special code")
            }
        }
        return rooms
    }

    private fun mapOffersPromotionsRoomContent(
        rooms: MutableList<PackageRoomTypes?>,
        response: CheckAvailabilityResponse,
        request: CheckAvailabilityInput,
    ) {
        log.info("mapOffersPromotionsRoomContent hotelId: ${request.hotelId}")
        rooms.forEach {
            response.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rt ->
                response.data.getHotelAvailability.contentLists?.rateList?.forEach { rateContent ->
                    response.data.getHotelAvailability.contentLists.policyList?.bookingPolicy?.forEach { bookingPolicy ->
                        response.data.getHotelAvailability.contentLists.policyList.cancelPolicy?.forEach { cancellationPolicy ->
                            if (rt.available == true && it?.roomCode == rt.product?.room?.code &&
                                rateContent.code == rt.product?.rate?.code &&
                                bookingPolicy?.code == rt.product?.bookingPolicy?.code &&
                                cancellationPolicy?.code == rt.product?.cancelPolicy?.code) {
                                when {
                                    (rt.couponApplies == true) -> {
                                        getOffersPromotionsRoomDetails(it, rt, rateContent, bookingPolicy, cancellationPolicy,response.data.getHotelAvailability.contentLists.policyList.commisionPolicy)
                                    }
                                    (request.agentId == rt.product?.refValue ||
                                            request.rateCode == rt.product?.refValue ||
                                            request.promoCode == rt.product?.refValue) -> {
                                        getOffersPromotionsRoomDetails(it, rt, rateContent, bookingPolicy, cancellationPolicy,response.data.getHotelAvailability.contentLists.policyList.commisionPolicy)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getOffersPromotionsRoomDetails(
        it: PackageRoomTypes?,
        rt: HudiniRoomType,
        rateContent: RateList,
        bookingPolicy: ContentBookingPolicy?,
        cancellationPolicy: ContentCancelPolicy?,
        commisionPolicy: List<ContentCommisionPolicy?>?
    ) {
        it?.rooms?.add(
            mapPackageDetails(
                rt,
                rateContent,
                bookingPolicy,
                cancellationPolicy,
                commisionPolicy
            )
        )
    }

    //merges check availability + offers || promotion rates
    private suspend fun getRates(request: CheckAvailabilityInput): HotelAvailabilityResponse? {
        log.info("getRates hotelId: ${request.hotelId}")
        val response: HotelAvailabilityResponse?
        val offersOrPromotionQuery = prepareGraphQlQuery(request, null)

        val normalRatesRes = CompletableFuture.supplyAsync {
            kotlinx.coroutines.runBlocking {
                future { getStandardMemberPackageAndMemberDealRates(request) }
            }
        }
        val offerOrPromotionRatesRes = CompletableFuture.supplyAsync {
            kotlinx.coroutines.runBlocking {
                future {
                    val offersOrPromotionRates = callCheckHotelAvailability(offersOrPromotionQuery)
                    getOffersOrPromotionRates(request, offersOrPromotionRates, null)
                }
            }
        }

        val normalRates = normalRatesRes.get().get()
        val offerOrPromotionRates = offerOrPromotionRatesRes.get().get()
        response = normalRates
        response?.roomAvailability?.offerRates = offerOrPromotionRates?.roomAvailability?.offerRates
        response?.roomAvailability?.promotionRates = offerOrPromotionRates?.roomAvailability?.promotionRates
        val existingTabs: MutableSet<String?>? = response?.tab?.toMutableSet()
        if (request.rateCode != null) {
            response?.tab?.clear()
            if (!response?.roomAvailability?.offerRates.isNullOrEmpty()) {
                response?.tab?.add(Constants.OFFERS)
            }
            existingTabs?.forEach {
                response.tab.add(it)
            }
        } else {
            response?.tab?.clear()
            if (!response?.roomAvailability?.promotionRates.isNullOrEmpty()) {
                response?.tab?.add(Constants.PROMOTIONS)
            }
            existingTabs?.forEach {
                response.tab.add(it)
            }
        }
        return sortRates(response)
    }

    //Prepares and returns graphQl Query based on request
    private fun prepareGraphQlQuery(request: CheckAvailabilityInput, rateFilterOrMemberDeal: String?): String {
        log.info("prepareGraphQlQuery hotelId: ${request.hotelId}")
        val jsonQuery: String?
        val rateFilter = request.rateFilter?.split(",")

        when {
            //rateFilter
            rateFilterOrMemberDeal == rateFilter?.get(1) -> {
                if (rateFilter?.get(0).equals(prop.chamberRateFilter)) {
                    val query = """
            query {
                getHotelAvailability(availability: {
                  hotelId: "${request.hotelId}",
                  startDate: "${request.startDate}", 
                  endDate: "${request.endDate}",
                  content: "${Constants.CONTENT}"
                  numRooms: ${request.numRooms}, 
                  adults: ${request.adults}, 
                  children: ${request.children}, 
                  onlyCheckRequested: "${Constants.ONLYCHECKREQUESTED}",
                  rateFilter: ["${rateFilter?.get(0)},${rateFilter?.get(1)}"],
                  memberTier: "${prop.memberTierChambers}",
                  package: "${request.packageFilter}"
                }) {
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
              rateFilterMap {
                code
                name
                description
                rateCodes
                rateCodeDescription
                rateCodeMapping {
                    key
                    value
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
                    log.info("_____GraphQl query prepared to call hudini check availability API with rateFilter and package_____")
                    jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
                } else {
                    val query = """
            query {
                getHotelAvailability(availability: {
                  hotelId: "${request.hotelId}",
                  startDate: "${request.startDate}", 
                  endDate: "${request.endDate}",
                  content: "${Constants.CONTENT}"
                  numRooms: ${request.numRooms}, 
                  adults: ${request.adults}, 
                  children: ${request.children}, 
                  onlyCheckRequested: "${Constants.ONLYCHECKREQUESTED}",
                  rateFilter: ["${rateFilter?.get(0)},${rateFilter?.get(1)}"],
                  memberTier: "${request.memberTier}",
                  package: "${request.packageFilter}"
                }) {
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
              rateFilterMap {
                code
                name
                description
                rateCodes
                rateCodeDescription
                rateCodeMapping {
                    key
                    value
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
                    log.info("_____GraphQl query prepared to call hudini check availability API with rateFilter and package_____")
                    jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
                }
            }
            //memberDeal
            rateFilterOrMemberDeal == rateFilter?.get(2) -> {
                val query = """
            query {
                getHotelAvailability(availability: {
                  hotelId: "${request.hotelId}",
                  startDate: "${request.startDate}", 
                  endDate: "${request.endDate}",
                  content: "${Constants.CONTENT}"
                  numRooms: ${request.numRooms}, 
                  adults: ${request.adults}, 
                  children: ${request.children}, 
                  onlyCheckRequested: "${Constants.ONLYCHECKREQUESTED}",
                  rateFilter: ["${rateFilter?.get(2)}"],
                }) {
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
              rateFilterMap {
                code
                name
                description
                rateCodes
                rateCodeDescription
                rateCodeMapping {
                    key
                    value
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
                log.info("_____GraphQl query prepared to call hudini check availability API with MemberDeal_____")
                jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
            }
            //rateCode
            request.promoCode.isNullOrEmpty() && request.promoType.isNullOrEmpty() && request.agentId.isNullOrEmpty() && request.agentType.isNullOrEmpty() && request.couponCode.isNullOrEmpty() -> {
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
                log.info("_____GraphQl query prepared to call hudini check availability API by rateCode_____")
                jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
            }
            //promocode
            rateFilterOrMemberDeal == Constants.PROMOCODE || request.rateCode.isNullOrEmpty() && request.agentId.isNullOrEmpty() && request.agentType.isNullOrEmpty() && request.couponCode.isNullOrEmpty() -> {
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
                log.info("_____GraphQl query prepared to call hudini check availability API by promoCode_____")
                jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
            }
            //couponCode
            rateFilterOrMemberDeal == Constants.COUPONCODE || request.rateCode.isNullOrEmpty() && request.promoCode.isNullOrEmpty() && request.promoType.isNullOrEmpty() && request.agentId.isNullOrEmpty() && request.agentType.isNullOrEmpty() -> {
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
                log.info("_____GraphQl query prepared to call hudini check availability API by coupon code_____")
                jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
            }
            //agentCode
            else -> {
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
                        commissionable
                      }
            			channelAccessOverridesList
                    }
                  }
                   policyList {
                      commisionPolicy {
                          code
                          description
                          commission{
                              value
                              unitType
                              calculationType
                          }
                      }
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
                log.info("_____GraphQl query prepared to call hudini check availability API by agentId_____")
                jsonQuery = createBooking.convertGraphQueryToJSONBody(query)
            }
        }
        log.debug("GraphQl query prepared as $jsonQuery")
        return jsonQuery
    }

    //mapping standard rates and member rates
    private fun mapRateFilterRates(
        request: CheckAvailabilityInput,
        rateFilterAndPackagesResponse: CheckAvailabilityResponse,
    ): HotelAvailabilityResponse? {
        log.info("mapRateFilterRates hotelId: ${request.hotelId}")
        val rateFilter = request.rateFilter?.split(",")
        val rateFilterRoomCodes = mutableSetOf<String>()
        val memberRateCode = mutableSetOf<String>()
        val mappedRateCodes = mutableSetOf<String?>()
        val unMappedRateCode = mutableSetOf<String?>()
        rateFilterAndPackagesResponse.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach {
            memberRateCode.add(
                it.product?.rate?.code.toString()
            )
            if (it.product?.refValue == rateFilter?.get(0)) {
                rateFilterRoomCodes.add(it.product?.room?.code.toString())
            }
        }
        rateFilterAndPackagesResponse.data?.getHotelAvailability?.rateFilterMap?.forEach { rfm ->
            if (rfm?.name == rateFilter?.get(0)) {
                rfm?.rateCodeMapping?.forEach {
                    mappedRateCodes.add(it.key)
                    mappedRateCodes.add(it.value)
                }
            }
        }
        rateFilterAndPackagesResponse.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rc ->
            if (rc.product?.rate?.code !in mappedRateCodes && rc.product?.refValue == rateFilter?.get(0)) {
                unMappedRateCode.add(rc.product?.rate?.code)
            }
        }
        log.debug("Total room codes in rate filters {}", rateFilterRoomCodes)
        log.debug("Total rate codes in rate filters {}", memberRateCode)
        log.debug("Mapped rate code {}", mappedRateCodes)
        log.debug("Unmapped rate codes {}", unMappedRateCode)
        val rooms: MutableList<RoomTypes?> = mutableListOf()
        //mapping room content and rooms to particular room code
        //creating room type objects
        for (i in 0 until rateFilterRoomCodes.size) {
            rooms.add(
                RoomTypes(
                    roomCode = rateFilterRoomCodes.toList()[i],
                    rooms = mutableListOf(),
                )
            )
        }

        //mapping Member rates
        mapMemberRates(rooms, rateFilterAndPackagesResponse, rateFilter)
        //mapping default std rates
        mapDefaultStandardRates(rooms, rateFilterAndPackagesResponse, unMappedRateCode)

        val res = HotelAvailabilityResponse(
            tab = mutableSetOf(Constants.ROOM_RATES, Constants.PACKAGES),
            hotelId = request.hotelId,
            synxisId = rateFilterAndPackagesResponse.data?.getHotelAvailability?.roomAvailability?.hotel?.id,
            couponCode = null,
            isCouponCodeValid = false,
            couponRemark = null,
            roomAvailability = Rates(
                roomRates = rooms,
                packagesRates = mutableListOf(),
                memberExclusiveRates = mutableListOf(),
                offerRates = mutableListOf(),
                promotionRates = mutableListOf()
            ),
            chargeList = rateFilterAndPackagesResponse.data?.getHotelAvailability?.contentLists?.chargeList,
            leastRestrictiveFailure = rateFilterAndPackagesResponse.data?.getHotelAvailability?.roomAvailability?.leastRestrictiveFailure,
            errorCode = rateFilterAndPackagesResponse.data?.getHotelAvailability?.errorCode,
            message = rateFilterAndPackagesResponse.data?.getHotelAvailability?.message
        )
        //Mapping standard rates
        mapStandardRates(rateFilterAndPackagesResponse, res, rateFilter)
        //Mapping content
        mapStdAndMemberContentDetails(res, rateFilterAndPackagesResponse)
        return sortRates(res)
    }

    private fun mapStdAndMemberContentDetails(
        res: HotelAvailabilityResponse,
        rateFilterAndPackagesResponse: CheckAvailabilityResponse,
    ) {
        log.info("mapStdAndMemberContentDetails hotelId: ${res.hotelId}")
        //Mapping content
        res.roomAvailability?.roomRates?.forEach {
            it?.rooms?.forEach { rooms ->
                rateFilterAndPackagesResponse.data?.getHotelAvailability?.contentLists?.rateList?.forEach { rateContent ->
                    when {
                        (rooms?.standardRate != null && rooms.memberRate != null && rateContent.code == rooms.standardRate?.rateCode) -> {
                            getStdContentDetails(rateFilterAndPackagesResponse, rooms, rateContent)
                        }

                        (rooms?.memberRate == null && rooms?.standardRate != null && rateContent.code == rooms.standardRate?.rateCode) -> {
                            getStdContentDetails(rateFilterAndPackagesResponse, rooms, rateContent)
                        }

                        (rooms?.standardRate == null && rooms?.memberRate != null && rateContent.code == rooms.memberRate?.rateCode) -> {
                            getMemberContentDetails(rateFilterAndPackagesResponse, rooms, rateContent)
                        }
                    }
                }
            }
        }
    }

    private fun getStdContentDetails(
        rateFilterAndPackagesResponse: CheckAvailabilityResponse,
        rooms: Rooms,
        rateContent: RateList,
    ) {
        var commissionPolicyDescription:ContentCommisionPolicy? = null
        if(rateFilterAndPackagesResponse.data?.getHotelAvailability?.contentLists?.policyList?.commisionPolicy?.isNullOrEmpty() != true &&
            rateContent.details?.indicators?.commissionable == true){
            commissionPolicyDescription = rateFilterAndPackagesResponse.data?.getHotelAvailability?.contentLists?.policyList?.commisionPolicy?.get(0)
        }
        rateFilterAndPackagesResponse.data?.getHotelAvailability?.contentLists?.policyList?.bookingPolicy?.forEach { bookingPolicy ->
            rateFilterAndPackagesResponse.data.getHotelAvailability.contentLists.policyList.cancelPolicy?.forEach { cancelPolicy ->
                if (rooms.standardRate?.bookingPolicyCode == bookingPolicy?.code && rooms.standardRate?.cancellationPolicyCode == cancelPolicy?.code) {
                    rooms.rateContent = rateContent
                    rooms.bookingPolicy = bookingPolicy
                    rooms.cancellationPolicy = cancelPolicy
                    rooms.commisionPolicy = commissionPolicyDescription
                }
            }
        }
    }

    private fun getMemberContentDetails(
        rateFilterAndPackagesResponse: CheckAvailabilityResponse,
        rooms: Rooms,
        rateContent: RateList,
    ) {
        var commissionPolicyDescription:ContentCommisionPolicy? = null
        if(rateFilterAndPackagesResponse.data?.getHotelAvailability?.contentLists?.policyList?.commisionPolicy?.isNullOrEmpty() != true &&
            rateContent.details?.indicators?.commissionable == true){
            commissionPolicyDescription = rateFilterAndPackagesResponse.data?.getHotelAvailability?.contentLists?.policyList?.commisionPolicy?.get(0)
        }
        rateFilterAndPackagesResponse.data?.getHotelAvailability?.contentLists?.policyList?.bookingPolicy?.forEach { bookingPolicy ->
            rateFilterAndPackagesResponse.data.getHotelAvailability.contentLists.policyList.cancelPolicy?.forEach { cancelPolicy ->
                if (rooms.memberRate?.bookingPolicyCode == bookingPolicy?.code && rooms.memberRate?.cancellationPolicyCode == cancelPolicy?.code) {
                    rooms.rateContent = rateContent
                    rooms.bookingPolicy = bookingPolicy
                    rooms.cancellationPolicy = cancelPolicy
                    rooms.commisionPolicy = commissionPolicyDescription
                }
            }
        }
    }

    private fun mapStandardRates(
        rateFilterAndPackagesResponse: CheckAvailabilityResponse,
        res: HotelAvailabilityResponse,
        rateFilter: List<String>?,
    ) {
        log.info("mapStandardRates hotelId: ${res.hotelId}")
        //Mapping standard rates
        rateFilterAndPackagesResponse.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rt ->
            res.roomAvailability?.roomRates?.forEach { rooms ->
                rooms?.rooms?.forEach { rc ->
                    rateFilterAndPackagesResponse.data.getHotelAvailability.rateFilterMap?.forEach { rfm ->
                        if (rfm?.name == rateFilter?.get(0)) {
                            rfm?.rateCodeMapping?.forEach { rcm ->
                                if (rc?.rateCode == rcm.value && rooms.roomCode == rt.product?.room?.code && rt.product?.rate?.code == rcm.key && rcm.key != rcm.value) {
                                    val standardRate = mapMemberDetails(rt.product?.rate?.code!!, rt)
                                    rc?.standardRate = standardRate
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun mapMemberRates(
        rooms: MutableList<RoomTypes?>,
        rateFilterAndPackagesResponse: CheckAvailabilityResponse,
        rateFilter: List<String>?,
    ) {
        //mapping Member rates
        rooms.forEach {
            rateFilterAndPackagesResponse.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rt ->
                rateFilterAndPackagesResponse.data.getHotelAvailability.contentLists?.rateList?.forEach { rateContent ->
                    rateFilterAndPackagesResponse.data.getHotelAvailability.contentLists.policyList?.bookingPolicy?.forEach { bookingPolicy ->
                        rateFilterAndPackagesResponse.data.getHotelAvailability.contentLists.policyList.cancelPolicy?.forEach { cancellationPolicy ->
                            getMemberRates(
                                rateContent,
                                rateFilterAndPackagesResponse,
                                rt,
                                it,
                                bookingPolicy,
                                cancellationPolicy,
                                rateFilter
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getMemberRates(
        rateContent: RateList,
        rateFilterAndPackagesResponse: CheckAvailabilityResponse,
        rt: HudiniRoomType,
        it: RoomTypes?,
        bookingPolicy: ContentBookingPolicy?,
        cancellationPolicy: ContentCancelPolicy?,
        rateFilter: List<String>?,
    ) {
        if (rateContent.code == rt.product?.rate?.code && it?.roomCode == rt.product?.room?.code) {
            rateFilterAndPackagesResponse.data?.getHotelAvailability?.rateFilterMap?.forEach { rfm ->
                if (rfm?.name == rateFilter?.get(0)) {
                    rfm?.rateCodeMapping?.forEach { rateCodeMapping ->
                        if (rateCodeMapping.value == rt.product?.rate?.code && rateCodeMapping.key != rateCodeMapping.value && bookingPolicy?.code == rt.product?.bookingPolicy?.code && cancellationPolicy?.code == rt.product?.cancelPolicy?.code) {
                            it?.rooms?.add(mapMemberRoomDetails(rt))
                        }
                    }
                }
            }
        }
    }

    private fun mapDefaultStandardRates(
        rooms: MutableList<RoomTypes?>,
        rateFilterAndPackagesResponse: CheckAvailabilityResponse,
        unMappedRateCode: MutableSet<String?>,
    ) {
        log.info("mapDefaultStandardRates, default std rate codes $unMappedRateCode")
        rooms.forEach {
            rateFilterAndPackagesResponse.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rt ->
                rateFilterAndPackagesResponse.data.getHotelAvailability.contentLists?.rateList?.forEach { rateContent ->
                    rateFilterAndPackagesResponse.data.getHotelAvailability.contentLists.policyList?.bookingPolicy?.forEach { bookingPolicy ->
                        rateFilterAndPackagesResponse.data.getHotelAvailability.contentLists.policyList.cancelPolicy?.forEach { cancellationPolicy ->
                            if (unMappedRateCode.isNotEmpty() && rateContent.code == rt.product?.rate?.code && it?.roomCode == rt.product?.room?.code && bookingPolicy?.code == rt.product?.bookingPolicy?.code && cancellationPolicy?.code == rt.product?.cancelPolicy?.code) {
                                unMappedRateCode.forEach { stdRates ->
                                    if (stdRates == rt.product?.rate?.code) {
                                        it?.rooms?.add(
                                            mapStandardRoomDetails1(
                                                rt
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //mapping package rates
    private fun mapPackagesRates(
        request: CheckAvailabilityInput,
        response: HotelAvailabilityResponse?,
        rateFilterAndPackagesResponse: CheckAvailabilityResponse,
    ): HotelAvailabilityResponse? {
        log.info("mapPackagesRates request: ${request.hotelId}")
        val rateFilter = request.rateFilter?.split(",")
        val rateFilterRoomCodes = mutableSetOf<String>()
        val memberRateCode = mutableSetOf<String>()

        rateFilterAndPackagesResponse.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach {
            if (it.product?.refValue == rateFilter?.get(1)) {
                rateFilterRoomCodes.add(it.product?.room?.code.toString())
            }
            memberRateCode.add(
                it.product?.rate?.code!!
            )
        }
        log.debug("Total room codes {}", rateFilterRoomCodes)
        log.debug("Total rate codes {}", memberRateCode)
        val rooms: MutableList<PackageRoomTypes?> = mutableListOf()
        //mapping room content and rooms to particular room code
        //creating room type objects
        for (i in 0 until rateFilterRoomCodes.size) {
            rooms.add(
                PackageRoomTypes(
                    roomCode = rateFilterRoomCodes.toList()[i],
                    rooms = mutableListOf(),
                )
            )
        }
        rooms.forEach {
            rateFilterAndPackagesResponse.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rt ->
                rateFilterAndPackagesResponse.data.getHotelAvailability.contentLists?.rateList?.forEach { rateContent ->
                    rateFilterAndPackagesResponse.data.getHotelAvailability.contentLists.policyList?.bookingPolicy?.forEach { bookingPolicy ->
                        rateFilterAndPackagesResponse.data.getHotelAvailability.contentLists.policyList.cancelPolicy?.forEach { cancellationPolicy ->
                            if (rateContent.code == rt.product?.rate?.code && bookingPolicy?.code == rt.product?.bookingPolicy?.code && cancellationPolicy?.code == rt.product?.cancelPolicy?.code && it?.roomCode == rt.product?.room?.code && rateFilter?.get(
                                    1
                                ) == rt.product?.refValue
                            ) {
                                it?.rooms?.add(
                                    mapPackageDetails(
                                        rt,
                                        rateContent,
                                        bookingPolicy!!,
                                        cancellationPolicy!!,
                                        rateFilterAndPackagesResponse.data.getHotelAvailability.contentLists.policyList.commisionPolicy
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        response?.roomAvailability?.packagesRates = rooms
        return sortRates(response)
    }

    //Mapping member exclusive rates
    private fun mapMemberDealRates(
        request: CheckAvailabilityInput,
        response: HotelAvailabilityResponse?,
        memberDeals: CheckAvailabilityResponse,
    ): HotelAvailabilityResponse? {
        log.info("mapMemberDealRates hotelId: ${request.hotelId}")
        val rateFilter = request.rateFilter?.split(",")
        val rateFilterRoomCodes = mutableSetOf<String>()
        val memberRateCode = mutableSetOf<String>()
        memberDeals.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach {
            if (it.product?.refValue == rateFilter?.get(2)) {
                rateFilterRoomCodes.add(it.product?.room?.code.toString())
            }
            memberRateCode.add(
                it.product?.rate?.code!!
            )
        }
        log.debug("Total room codes {}", rateFilterRoomCodes)
        log.debug("Total rate codes {}", memberRateCode)
        val rooms: MutableList<PackageRoomTypes?> = mutableListOf()
        //mapping room content and rooms to particular room code
        //creating room type objects
        for (i in 0 until rateFilterRoomCodes.size) {
            rooms.add(
                PackageRoomTypes(
                    roomCode = rateFilterRoomCodes.toList()[i],
                    rooms = mutableListOf(),
                )
            )
        }
        log.info("rate filters size ${rateFilter?.size}")
        rooms.forEach {
            memberDeals.data?.getHotelAvailability?.roomAvailability?.roomTypes?.forEach { rt ->
                memberDeals.data.getHotelAvailability.contentLists?.rateList?.forEach { rateContent ->
                    memberDeals.data.getHotelAvailability.contentLists.policyList?.bookingPolicy?.forEach { bookingPolicy ->
                        memberDeals.data.getHotelAvailability.contentLists.policyList.cancelPolicy?.forEach { cancellationPolicy ->
                            if (rateContent.code == rt.product?.rate?.code && bookingPolicy?.code == rt.product?.bookingPolicy?.code && cancellationPolicy?.code == rt.product?.cancelPolicy?.code && it?.roomCode == rt.product?.room?.code && rateFilter?.get(
                                    2
                                ) == rt.product?.refValue
                            ) {
                                it?.rooms?.add(
                                    mapPackageDetails(
                                        rt,
                                        rateContent,
                                        bookingPolicy!!,
                                        cancellationPolicy!!,
                                        memberDeals.data.getHotelAvailability.contentLists.policyList.commisionPolicy
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        response?.roomAvailability?.memberExclusiveRates = rooms
        return sortRates(response)
    }

    /* This method is used to map member rates and returns rooms object */
    private fun mapMemberRoomDetails(
        response: HudiniRoomType,
    ): Rooms {
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
        return Rooms(
            rateCode = response.product.rate?.code!!,
            isCommissionable = false,
            rateContent = null,
            bookingPolicy = null,
            cancellationPolicy = null,
            commisionPolicy = null,
            memberRate = StandardRate(
                response.available,
                response.availableInventory,
                response.product.rate.code,
                response.product.bookingPolicy?.code,
                response.product.cancelPolicy?.code,
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
                ),
                perNight = response.product.prices.perNight,
                daily = response.product.prices.daily
            ),
            standardRate = null
        )
    }

    /* This method is used to map default std rates and returns rooms object */
    private fun mapStandardRoomDetails1(
        response: HudiniRoomType,
    ): Rooms {
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
        return Rooms(
            rateCode = response.product.rate?.code!!,
            isCommissionable = false,
            rateContent = null,
            bookingPolicy = null,
            cancellationPolicy = null,
            commisionPolicy = null,
            standardRate = StandardRate(
                response.available,
                response.availableInventory,
                response.product.rate.code,
                response.product.bookingPolicy?.code,
                response.product.cancelPolicy?.code,
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
                ),
                perNight = response.product.prices.perNight,
                daily = response.product.prices.daily,
            ),
            memberRate = null
        )
    }

    /* This method prepares and returns the standard rate object to map for particular room */
    private fun mapMemberDetails(memberRateCode: String, res: HudiniRoomType): StandardRate {
        var commissionPolicyDescription:ContentCommisionPolicy? = null
        val breakDown: MutableList<FEBreakDown> =
            res.product?.prices?.totalPrice?.price?.tax?.breakDown!!.map {
                FEBreakDown(
                    amount = it?.amount,
                    code = it?.code,
                    isInclusive = it?.isInclusive,
                    isPayAtProperty = it?.isPayAtProperty,
                    isPerStay = it?.isPerStay
                )
            }.toMutableList()
        return StandardRate(
            res.available,
            res.availableInventory,
            memberRateCode,
            res.product.bookingPolicy?.code,
            res.product.cancelPolicy?.code,
            res.product.prices.daily,
            res.product.prices.perNight,
            tax = FETax(
                amount = res.product.prices.totalPrice.price.tax.amount!!,
                breakDown = breakDown
            ),
            Total(
                res.product.prices.totalPrice.price.total?.amount,
                res.product.prices.totalPrice.price.total?.amountPayAtProperty,
                res.product.prices.totalPrice.price.total?.amountPayableNow,
                res.product.prices.totalPrice.price.total?.amountWithInclusiveTaxes,
                res.product.prices.totalPrice.price.total?.amountWithTaxesFees,
                res.product.prices.totalPrice.price.total?.amountWithFees
            )
        )
    }

    //Mapping package rates
    private fun mapPackageDetails(
        res: HudiniRoomType,
        rateContent: RateList?,
        booingPolicy: ContentBookingPolicy?,
        cancellationPolicy: ContentCancelPolicy?,
        commisionPolicy: List<ContentCommisionPolicy?>?
    ): PackageRooms {
        var commissionPolicyDescription:ContentCommisionPolicy? = null
        if(commisionPolicy?.isNullOrEmpty() != true && rateContent?.details?.indicators?.commissionable == true){
            commissionPolicyDescription = commisionPolicy[0]
        }
        return PackageRooms(
            res.product?.rate?.code,
            res.available,
            rateContent?.details?.indicators?.commissionable,
            res.availableInventory,
            res.product?.prices?.daily,
            res.product?.prices?.perNight,
            Tax(
                res.product?.prices?.totalPrice?.price?.tax?.amount,
                res.product?.prices?.totalPrice?.price?.tax?.breakDown
            ),
            Total(
                res.product?.prices?.totalPrice?.price?.total?.amount,
                res.product?.prices?.totalPrice?.price?.total?.amountPayAtProperty,
                res.product?.prices?.totalPrice?.price?.total?.amountPayableNow,
                res.product?.prices?.totalPrice?.price?.total?.amountWithInclusiveTaxes,
                res.product?.prices?.totalPrice?.price?.total?.amountWithTaxesFees,
                res.product?.prices?.totalPrice?.price?.total?.amountWithFees
            ),
            rateContent,
            booingPolicy,
            cancellationPolicy,
            commissionPolicyDescription
        )
    }

}