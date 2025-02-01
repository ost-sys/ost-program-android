package com.ost.application

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ost.application.databinding.ActivityPreferencesBinding
import dev.oneuiproject.oneui.layout.ToolbarLayout
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ChangelogActivity : AppCompatActivity() {

    private lateinit var changelogListView: ListView
    private lateinit var toolbarLayout: ToolbarLayout

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changelog)

        changelogListView = findViewById(R.id.changelogListView)

        toolbarLayout = findViewById(R.id.toolbarLayout)
        toolbarLayout.showNavigationButtonAsBack = true

        fetchChangelog()
    }

    private fun fetchChangelog() {
        val url = "https://api.github.com/repos/ost-sys/ost-program-android/releases"
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    val errorList = listOf(mapOf("title" to getString(R.string.error), "subtitle" to e.message))
                    val adapter = SimpleAdapter(
                        this@ChangelogActivity,
                        errorList,
                        android.R.layout.simple_list_item_2,
                        arrayOf("title", "subtitle"),
                        intArrayOf(android.R.id.text1, android.R.id.text2)
                    )
                    changelogListView.adapter = adapter
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val changelog = parseChangelog(responseBody)
                        runOnUiThread {
                            val adapter = SimpleAdapter(
                                this@ChangelogActivity,
                                changelog,
                                android.R.layout.simple_list_item_2,
                                arrayOf("title", "subtitle"),
                                intArrayOf(android.R.id.text1, android.R.id.text2)
                            )
                            changelogListView.adapter = adapter

                            changelogListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                                val release = changelog[position]
                                val downloadUrl = release["download_url"]
                                if (downloadUrl != null) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                    startActivity(intent)
                                }
                            }
                        }
                    }
                } else {
                    runOnUiThread {
                        val errorList = listOf(mapOf("title" to getString(R.string.error), "subtitle" to response.message))
                        val adapter = SimpleAdapter(
                            this@ChangelogActivity,
                            errorList,
                            android.R.layout.simple_list_item_2,
                            arrayOf("title", "subtitle"),
                            intArrayOf(android.R.id.text1, android.R.id.text2)
                        )
                        changelogListView.adapter = adapter
                    }
                }
            }
        })
    }

    private fun parseChangelog(json: String): List<Map<String, String>> {
        val releases = JSONArray(json)
        val changelogList = mutableListOf<Map<String, String>>()

        for (i in 0 until releases.length()) {
            val release = releases.getJSONObject(i)
            val updateName = release.getString("name")
            val version = release.getString("tag_name")
            val body = release.getString("body")
            val assets = release.getJSONArray("assets")

            val downloadUrl = if (assets.length() > 0) {
                assets.getJSONObject(0).getString("browser_download_url")
            } else {
                null
            }

            changelogList.add(
                mapOf(
                    "title" to "${getString(R.string.version)} $version (${updateName})",
                    "subtitle" to body,
                    "download_url" to (downloadUrl ?: "")
                )
            )
        }

        return changelogList
    }
}