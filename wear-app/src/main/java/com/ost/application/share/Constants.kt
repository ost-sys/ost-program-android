package com.ost.application.share

object Constants {
    const val SERVICE_TYPE = "_ost_filetransfer._tcp."
    const val SERVICE_NAME_PREFIX = "OST_Share_"
    const val TRANSFER_PORT = 55667
    const val TAG = "OST_Share"

    const val FILES_DIR = "OST"

    const val CMD_REQUEST_SEND = "REQ_SEND"
    const val CMD_ACCEPT = "ACCEPT"
    const val CMD_REJECT = "REJECT"
    const val CMD_SEPARATOR = "|"

    const val KEY_DEVICE_TYPE = "devtype"
    const val VALUE_DEVICE_PHONE = "phone"
    const val VALUE_DEVICE_WATCH = "watch"
    const val VALUE_DEVICE_UNKNOWN = "unknown"
}