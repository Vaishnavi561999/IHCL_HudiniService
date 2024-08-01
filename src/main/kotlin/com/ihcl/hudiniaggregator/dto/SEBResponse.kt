package com.ihcl.hudiniaggregator.dto

import com.google.gson.annotations.SerializedName

data class SEBResponse(
    @SerializedName("ResponseCode")
    val responseCode: String?,
    @SerializedName("ResponseMessage")
    val responseMessage: String?,
    @SerializedName("MyTajREQID")
    val myTajREQID: String?,
    @SerializedName("StartDate")
    val startDate: String?,
    @SerializedName("EndDate")
    val endDate: String?,
    @SerializedName("ApprovedRooms")
    val approvedRooms: String?,
    @SerializedName("CRSReferenceNumber")
    val crsReferenceNumber: String?,
    @SerializedName("CRSTimeStamp")
    val crsTimeStamp: String?,
    @SerializedName("EmailID")
    val emailID: String?,
    @SerializedName("MobileNumber")
    val mobileNumber: String?,
    @SerializedName("NumberofPerson")
    val numberofPerson: String?,
    @SerializedName("CRSRequestID")
    val crsRequestID: String?,
    @SerializedName("HotelName")
    val hotelName: String?,
    @SerializedName("CreatedOn")
    val createdOn: String?,
    @SerializedName("ModifiedOn")
    val modifiedOn: String?
)