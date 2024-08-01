package com.ihcl.hudiniaggregator.dto.socialMediaFeed

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class InstagramFeedResponse(
    @SerializedName("\$type")
    val type: String?,
    val content: String?,
    @SerializedName("Data")
    val `data`: List<Data?>?,
    @SerializedName("Message")
    val message: String?,
    @SerializedName("Success")
    val success: Boolean?
)
@Serializable
data class Data(
    @SerializedName("AttachmentMetadata")
    val attachmentMetadata: AttachmentMetadata?,
    @Serializable
    val author: Author?,
    val brandInfo: BrandInfo?,
    val caption: String?,
    val channelGroup: Int?,
    val channelType: Int?,
    val concreteClassName: String?,
    val description: String?,
    val mediaContents: String?,
    val mediaType: Int?,
    val mediaTypeFormat: Int?,
    val mentionID: String?,
    val mentionMetadata: String?,
    val mentionTime: String?,
    val mentionTimeEpoch: Double?,
    val note: String?,
    val numberOfMentions: String?,
    val sentiment: Int?,
    val status: Int?,
    val tagID: Int?,
    val ticketID: Int?,
    val title: String?,
    @SerializedName("Url")
    val url: String?
)
@Serializable
data class AttachmentMetadata(
    val attachments: String?,
    val mediaAttachments: List<String?>?,
    val mediaContentText: String?,
    val mediaContents: MutableList<MediaContent?>?,
    val mediaUrls: String?
)
@Serializable
data class LocoBuzzCRMDetails(
    val address1: String?,
    val address2: String?,
    val age: Int?,
    val alternatePhoneNumber: String?,
    val alternativeEmailID: String?,
    val city: String?,
    val country: String?,
    val customCRMColumnXml: String?,
    val customCRMXml: String?,
    val dob: String?,
    val dobString: String?,
    val emailID: String?,
    val gender: String?,
    val id: Int?,
    val isInserted: Boolean?,
    val link: String?,
    val modifiedByUser: String?,
    val modifiedDateTime: String?,
    val modifiedTime: String?,
    val modifiedTimeEpoch: Double?,
    val name: String?,
    val notes: String?,
    val phoneNumber: String?,
    val ssn: String?,
    val state: String?,
    val timeoffset: Int?,
    val zipCode: String?
)
@Serializable
data class MediaContent(
    val displayName: String?,
    val errorMessage: String?,
    val expiryDate: String?,
    val keyname: String?,
    val mediaTags: String?,
    val mediaTagsList: String?,
    val mediaType: Int?,
    val mediaUrl: String?,
    val name: String?,
    val profilePicUrl: String?,
    val rating: Int?,
    val tempMediaID: String?,
    val thumbUrl: String?
)
@Serializable
data class BrandInfo(
    val brandColor: String?,
    val brandFriendlyName: String?,
    val brandGroupName: String?,
    val brandID: Int?,
    val brandIDs: String?,
    val brandLogo: String?,
    val brandName: String?,
    val categoryGroupID: Int?,
    val categoryID: Int?,
    val categoryName: String?,
    val compititionBrandIDs: String?,
    val isBrandworkFlowEnabled: Boolean?,
    val mainBrandID: Int?
)
@Serializable
data class Author(
    val canBeMarkedInfluencer: Boolean?,
    val canHaveConnectedUsers: Boolean?,
    val canHaveUserTags: Boolean?,
    val channel: Int?,
    val channelGroup: Int?,
    val connectedUsers: List<String?>?,
    val crmColumns: String?,
    val firstActivity: String?,
    val followersCount: Int?,
    val glbMarkedInfluencerCategoryID: Int?,
    val glbMarkedInfluencerCategoryName: String?,
    val id: Int?,
    val interactionCount: Int?,
    val isAnonymous: Boolean?,
    val isMarkedInfluencer: Boolean?,
    val isVerifed: Boolean?,
    val lastActivity: String?,
    val latestTicketID: String?,
    val location: String?,
    val locationXML: String?,
    val locoBuzzCRMDetails: LocoBuzzCRMDetails?,
    val markedInfluencerCategoryID: Int?,
    val markedInfluencerCategoryName: String?,
    val markedInfluencerID: Int?,
    val markedInfluencers: List<String?>?,
    val name: String?,
    val nps: Int?,
    val picUrl: String?,
    val previousLocoBuzzCRMDetails: String?,
    val profileImage: String?,
    val profileUrl: String?,
    val screenname: String?,
    val sentimentUpliftScore: Double?,
    val socialId: String?,
    val ticketLevelAttributes: String?,
    val url: String?,
    val userSentiment: Int?,
    val userTags: List<String?>?
)
@Serializable
data class  FeedResponseDTO(
    val success: Boolean?,
    val data: List<ResData?>?
)
@Serializable
data class ResData(
    val redirectionURL:String?,
    val attachmentMetadata: AttachmentMetadata?
)
