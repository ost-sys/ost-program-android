package com.ost.application.share

import android.net.Uri
import android.net.nsd.NsdServiceInfo

object Constants {
    const val SERVICE_TYPE = "_ost_filetransfer._tcp."
    const val SERVICE_NAME_PREFIX = "OST_Share_"
    const val KEY_DEVICE_TYPE = "devtype"
    const val VALUE_DEVICE_PHONE = "phone"
    const val VALUE_DEVICE_WATCH = "watch"
    const val VALUE_DEVICE_UNKNOWN = "unknown"

    const val TRANSFER_PORT = 55667

    const val CMD_SEPARATOR = "|"
    const val CMD_REQUEST_SEND = "REQ_SEND"
    const val CMD_REQUEST_SEND_MULTI = "SEND_MULTI_REQUEST"
    const val FILE_META = "FILE_META"
    const val CMD_ACCEPT = "ACCEPT"
    const val CMD_REJECT = "REJECT"

    const val FILES_DIR = "OST"

    const val NOTIFICATION_CHANNEL_ID = "weartransfer_channel"
    const val NOTIFICATION_ID_TRANSFER = 1001
    const val NOTIFICATION_ID_SERVICE_FOREGROUND = 1003
    const val NOTIFICATION_ID_COMPLETION = 1002 // For completion notifications
    const val NOTIFICATION_ID_INCOMING_FILE = 1004 // For incoming file request notification

    const val ACTION_START_SERVICE = "com.ost.application.share.ACTION_START_SERVICE"
    const val ACTION_STOP_SERVICE = "com.ost.application.share.ACTION_STOP_SERVICE"
    const val ACTION_START_DISCOVERY = "com.ost.application.share.ACTION_START_DISCOVERY"
    const val ACTION_STOP_DISCOVERY = "com.ost.application.share.ACTION_STOP_DISCOVERY"
    const val ACTION_SEND_FILES = "com.ost.application.share.ACTION_SEND_FILES"
    const val ACTION_CANCEL_TRANSFER = "com.ost.application.share.ACTION_CANCEL_TRANSFER"
    const val ACTION_ACCEPT_RECEIVE = "com.ost.application.share.ACTION_ACCEPT_RECEIVE"
    const val ACTION_REJECT_RECEIVE = "com.ost.application.share.ACTION_REJECT_RECEIVE"
    const val EXTRA_FILE_URIS = "extra_file_uris"
    const val EXTRA_TARGET_DEVICE = "extra_target_device"
    const val EXTRA_REQUEST_ID = "extra_request_id"

    const val SENT_PREFIX = "SENT"
    const val RECEIVING_PREFIX = "RECEIVING"
    const val CANCELLED_KEYWORD = "CANCELED"
    const val ERROR_PREFIX = "ERROR"

    const val TAG = "OST_Share"
    const val CONFIRMATION_TIMEOUT_MILLIS = 3000L
}

data class DiscoveredDevice(
    val serviceInfo: NsdServiceInfo,
    var isResolved: Boolean = false,
    var isResolving: Boolean = false
) : android.os.Parcelable {
    constructor(parcel: android.os.Parcel) : this(
        parcel.readParcelable(NsdServiceInfo::class.java.classLoader)!!,
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    )

    val id: String get() = serviceInfo.serviceName ?: "UnknownId"
    val name: String get() = extractDeviceName(serviceInfo.serviceName ?: "Unknown Device")
    val host: String? get() = if (isResolved) serviceInfo.host?.hostAddress else null
    val port: Int? get() = if (isResolved) serviceInfo.port else null
    val deviceType: String get() = serviceInfo.attributes[Constants.KEY_DEVICE_TYPE]?.toString(Charsets.UTF_8) ?: Constants.VALUE_DEVICE_UNKNOWN

    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeParcelable(serviceInfo, flags)
        parcel.writeByte(if (isResolved) 1 else 0)
        parcel.writeByte(if (isResolving) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : android.os.Parcelable.Creator<DiscoveredDevice> {
        override fun createFromParcel(parcel: android.os.Parcel): DiscoveredDevice {
            return DiscoveredDevice(parcel)
        }

        override fun newArray(size: Int): Array<DiscoveredDevice?> {
            return arrayOfNulls(size)
        }

        fun extractDeviceName(serviceName: String): String {
            return serviceName
                .removePrefix(Constants.SERVICE_NAME_PREFIX)
                .substringBeforeLast('_')
                .replace("_", " ")
                .trim()
                .ifBlank { serviceName }
        }
    }
}

data class FileTransferInfo(
    val uri: Uri?,
    val name: String,
    val size: Long
)