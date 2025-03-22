package com.ost.application.activity.changelog

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ost.application.R
import dev.oneuiproject.oneui.layout.ToolbarLayout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException

class ChangelogActivity : AppCompatActivity() {

    private lateinit var changelogRecyclerView: RecyclerView
    private lateinit var toolbarLayout: ToolbarLayout

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changelog)

        toolbarLayout = findViewById(R.id.toolbarLayout)
        toolbarLayout.showNavigationButtonAsBack = true

        changelogRecyclerView = findViewById(R.id.changelogRecyclerView)
        changelogRecyclerView.layoutManager = LinearLayoutManager(this)

        fetchChangelog()
    }

    private fun fetchChangelog() {
        val url = "https://api.github.com/repos/ost-sys/ost-program-android/releases"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    changelogRecyclerView.adapter = ChangelogAdapter(
                        listOf(ChangelogItem(getString(R.string.error), e.message ?: "", null))
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val changelog = parseChangelog(responseBody)
                        runOnUiThread {
                            changelogRecyclerView.adapter = ChangelogAdapter(changelog)
                        }
                    }
                } else {
                    runOnUiThread {
                        changelogRecyclerView.adapter = ChangelogAdapter(
                            listOf(ChangelogItem(getString(R.string.error), response.message, null))
                        )
                    }
                }
            }
        })
    }

    private fun parseChangelog(json: String): List<ChangelogItem> {
        val releases = JSONArray(json)
        val changelogList = mutableListOf<ChangelogItem>()

        for (i in 0 until releases.length()) {
            val release = releases.getJSONObject(i)
            val version = release.getString("tag_name")
            val body = release.getString("body")
            val assets = release.getJSONArray("assets")

            val downloadUrl = if (assets.length() > 0) {
                assets.getJSONObject(0).getString("browser_download_url")
            } else {
                null
            }

            changelogList.add(ChangelogItem(version, body, downloadUrl))
        }

        return changelogList
    }
}