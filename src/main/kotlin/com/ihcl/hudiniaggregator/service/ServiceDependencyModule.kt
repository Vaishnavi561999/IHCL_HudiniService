package com.ihcl.hudiniaggregator.service

import com.ihcl.hudiniaggregator.util.GenerateToken
import org.koin.dsl.module

val serviceModule = module {
    single {
        BookingDetailsService()
    }
    single {
        CreateBookingService()
    }
    single {
        CancelBookingService()
    }
    single {
        SocialMediaFeedService()
    }
    single {
        UpdateBookingService()
    }
    single {
        GetHotelLeadAvailabilityCalenderService()
    }
    single {
        RateCodePromoCodeService()
    }
    single {
        DestinationAvailabilityService()
    }
    single {
        RateCodeCheckAvailabilityService()
    }
    single {
        CheckAvailabilityService()
    }
    single {
        GenerateToken()
    }
    single {
        SEBService()
    }
}