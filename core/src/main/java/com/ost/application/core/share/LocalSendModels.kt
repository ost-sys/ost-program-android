package com.ost.application.core.share

import com.google.gson.annotations.SerializedName

data class LocalSendInfo(
    @SerializedName("alias") val alias: String,
    @SerializedName("version") val version: String,
    @SerializedName("deviceModel") val deviceModel: String?,
    @SerializedName("deviceType") val deviceType: String?,
    @SerializedName("fingerprint") val fingerprint: String,
    @SerializedName("port") val port: Int,
    @SerializedName("protocol") val protocol: String,
    @SerializedName("download") val download: Boolean
)

data class LocalSendFileMeta(
    @SerializedName("id") val id: String,
    @SerializedName("fileName") val fileName: String,
    @SerializedName("size") val size: Long,
    @SerializedName("fileType") val fileType: String,
    @SerializedName("sha256") val sha256: String? = null,
    @SerializedName("preview") val preview: String? = null
)

data class LocalSendPrepareRequest(
    @SerializedName("info") val info: LocalSendInfo,
    @SerializedName("files") val files: Map<String, LocalSendFileMeta>
)

data class LocalSendPrepareResponse(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("files") val files: Map<String, String>
)

enum class LocalSendDeviceType(val value: String) {
    MOBILE("mobile"),
    DESKTOP("desktop"),
    WEB("web"),
    HEADLESS("headless"),
    SERVER("server")
}
