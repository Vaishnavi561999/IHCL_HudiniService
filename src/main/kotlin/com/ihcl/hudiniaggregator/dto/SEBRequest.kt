package com.ihcl.hudiniaggregator.dto

data class RequestToSEB(
    val userName: String?,
    val password: String?,
    val domain: String?,
    val myTajREQID: String?,
    val startDate: String?,
    val endDate: String?,
    val approvedRooms: String?,
    val crsReferenceNumber: String?,
    val crsTimeStamp: String?,
    val emailID: String?,
    val mobileNumber: String?,
    val numberOfPerson: String?,
    val crsRequestID: String?,
    val hotelName: String?,
)

data class SEBRequest(
    val myTajREQID: String?,
    val startDate: String?,
    val endDate: String?,
    val approvedRooms: String?,
    val crsReferenceNumber: String?,
    val crsTimeStamp: String?,
    val emailID: String?,
    val mobileNumber: String?,
    val numberOfPerson: String?,
    val crsRequestID: String?,
    val hotelName: String?,
)
