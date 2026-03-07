package com.ost.application.ui.screen.share

import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.os.Parcel
import android.os.Parcelable
import java.net.InetAddress

object Constants {
    const val SERVICE_TYPE = "_ost_filetransfer._tcp."
    const val SERVICE_NAME_PREFIX = "OST_Share_"
    const val TRANSFER_PORT = 55667
    const val TAG = "OST_Share"
    const val FILE_PROVIDER_AUTHORITY = "com.ost.application.fileprovider"

    const val FILES_DIR = "OST"

    const val CMD_REQUEST_SEND = "REQ_SEND"
    const val CMD_REQUEST_SEND_MULTI = "SEND_MULTI_REQUEST"
    const val FILE_META = "FILE_META"
    const val CMD_ACCEPT = "ACCEPT"
    const val CMD_REJECT = "REJECT"
    const val CMD_SEPARATOR = "|"

    const val KEY_DEVICE_TYPE = "devtype"
    const val VALUE_DEVICE_PHONE = "phone"
    const val VALUE_DEVICE_WATCH = "watch"
    const val VALUE_DEVICE_UNKNOWN = "unknown"

    const val NOTIFICATION_CHANNEL_ID = "ost_file_transfer_channel"
    const val NOTIFICATION_CHANNEL_ID_LIVE_UPDATES = "ost_file_transfer_live_updates"
    const val NOTIFICATION_CHANNEL_ID_COMPLETION = "ost_file_transfer_completion_channel"
    const val NOTIFICATION_CHANNEL_ID_INCOMING = "ost_file_transfer_incoming_channel"

    const val NOTIFICATION_ID_TRANSFER = 11223
    const val NOTIFICATION_ID_FOREGROUND_SERVICE = 11224
    const val NOTIFICATION_ID_INCOMING_FILE = 11225

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
    const val INCOMING_REQUEST_TIMEOUT_MS = 30000L
}

data class DiscoveredDevice(
    val id: String,
    var name: String,
    var type: String = Constants.VALUE_DEVICE_UNKNOWN,
    var isResolved: Boolean = false,
    var ipAddress: InetAddress? = null,
    var port: Int = -1,
    var isResolving: Boolean = false
) : Parcelable {

    constructor(serviceInfo: NsdServiceInfo) : this(
        id = serviceInfo.serviceName,
        name = extractDeviceName(serviceInfo.serviceName),
        type = serviceInfo.attributes?.get(Constants.KEY_DEVICE_TYPE)?.let { String(it, Charsets.UTF_8) } ?: Constants.VALUE_DEVICE_UNKNOWN,
        isResolved = (serviceInfo.host != null && serviceInfo.port > 0),
        ipAddress = serviceInfo.host,
        port = serviceInfo.port,
        isResolving = false
    )

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readByte() != 0.toByte(),
        parcel.readSerializable() as? InetAddress,
        parcel.readInt(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(type)
        parcel.writeByte(if (isResolved) 1 else 0)
        parcel.writeSerializable(ipAddress)
        parcel.writeInt(port)
        parcel.writeByte(if (isResolving) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DiscoveredDevice> {
        override fun createFromParcel(parcel: Parcel): DiscoveredDevice {
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