package com.ost.application.core.share

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ReceivedFileEntry(val fileName: String, val timestamp: Long)

object ReceivedFilesLedger {
    private const val TAG = "ReceivedFilesLedger"
    private const val LEDGER_FILE_NAME = ".ost_received_ledger.json"

    fun getValidEntries(dir: File): List<ReceivedFileEntry> {
        val ledgerFile = File(dir, LEDGER_FILE_NAME)
        if (!ledgerFile.exists()) return emptyList()

        val validEntries = mutableListOf<ReceivedFileEntry>()
        var needsRewrite = false

        try {
            val content = ledgerFile.readText()
            val jsonArray = JSONArray(content)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val fileName = jsonObject.getString("fileName")
                val timestamp = jsonObject.getLong("timestamp")

                val actualFile = File(dir, fileName)
                if (actualFile.exists()) {
                    validEntries.add(ReceivedFileEntry(fileName, timestamp))
                } else {
                    needsRewrite = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read ledger", e)
            return emptyList()
        }

        if (needsRewrite) {
            writeEntries(ledgerFile, validEntries)
        }

        return validEntries.sortedByDescending { it.timestamp }
    }

    fun addEntry(dir: File, fileName: String, timestamp: Long = System.currentTimeMillis()) {
        val ledgerFile = File(dir, LEDGER_FILE_NAME)
        val currentEntries = if (ledgerFile.exists()) {
            try {
                val content = ledgerFile.readText()
                val jsonArray = JSONArray(content)
                val list = mutableListOf<ReceivedFileEntry>()
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    list.add(ReceivedFileEntry(jsonObject.getString("fileName"), jsonObject.getLong("timestamp")))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }.toMutableList()

        currentEntries.removeAll { it.fileName == fileName }
        currentEntries.add(ReceivedFileEntry(fileName, timestamp))

        writeEntries(ledgerFile, currentEntries)
    }

    fun removeEntry(dir: File, fileName: String) {
        val ledgerFile = File(dir, LEDGER_FILE_NAME)
        if (!ledgerFile.exists()) return

        val entries = getValidEntries(dir).toMutableList()
        val removed = entries.removeAll { it.fileName == fileName }
        if (removed) {
            writeEntries(ledgerFile, entries)
        }
    }

    private fun writeEntries(file: File, entries: List<ReceivedFileEntry>) {
        try {
            val jsonArray = JSONArray()
            for (entry in entries) {
                val obj = JSONObject()
                obj.put("fileName", entry.fileName)
                obj.put("timestamp", entry.timestamp)
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write ledger", e)
        }
    }
}
