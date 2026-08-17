package com.ost.application.core.share

import android.content.Context
import android.net.Uri
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.gson.gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class LocalSendClient {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            gson {
                serializeNulls()
            }
        }
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis  = 120_000
        }
        engine {
            config {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())

                sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                hostnameVerifier { _, _ -> true }

                connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }

    private val fastClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            gson {
                serializeNulls()
            }
        }
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 3_000
            connectTimeoutMillis = 1_500
            socketTimeoutMillis  = 3_000
        }
        engine {
            config {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())
                sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                hostnameVerifier { _, _ -> true }

                connectTimeout(1200, java.util.concurrent.TimeUnit.MILLISECONDS)
                readTimeout(2000, java.util.concurrent.TimeUnit.MILLISECONDS)
            }
        }
    }

    private val gson = com.google.gson.Gson()

    suspend fun fetchInfo(ip: String, port: Int): LocalSendInfo? {
        for (scheme in listOf("https", "http")) {
            try {
                val response = fastClient.get("$scheme://$ip:$port/api/localsend/v2/info")
                if (response.status.isSuccess()) {
                    val text = response.bodyAsText()
                    return gson.fromJson(text, LocalSendInfo::class.java)
                }
            } catch (_: Exception) {}
        }
        return null
    }

    suspend fun register(ip: String, port: Int, selfInfo: LocalSendInfo): Boolean {
        val payload = gson.toJson(selfInfo)
        for (scheme in listOf("https", "http")) {
            try {
                val response = fastClient.post("$scheme://$ip:$port/api/localsend/v2/register") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
                if (response.status.isSuccess()) return true
            } catch (_: Exception) {}
        }
        return false
    }

    suspend fun prepareUpload(ip: String, port: Int, request: LocalSendPrepareRequest): LocalSendPrepareResponse? {
        val payload = gson.toJson(request)
        for (scheme in listOf("https", "http")) {
            try {
                android.util.Log.d("LocalSendClient", "prepareUpload -> $scheme://$ip:$port/api/localsend/v2/prepare-upload")
                val response = client.post("$scheme://$ip:$port/api/localsend/v2/prepare-upload") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
                android.util.Log.d("LocalSendClient", "prepareUpload response: ${response.status}")
                if (response.status.isSuccess()) {
                    val text = response.bodyAsText()
                    return gson.fromJson(text, LocalSendPrepareResponse::class.java)
                } else {
                    android.util.Log.w("LocalSendClient", "prepareUpload returned non-success: ${response.status}")
                }
            } catch (e: Exception) {
                android.util.Log.e("LocalSendClient", "prepareUpload error ($scheme): ${e.message}", e)
            }
        }
        return null
    }

    suspend fun uploadFile(
        context: Context,
        ip: String,
        port: Int,
        sessionId: String,
        fileId: String,
        token: String,
        uri: Uri,
        onProgress: ((Long) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bytes: ByteArray = context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(8192)
                val out = java.io.ByteArrayOutputStream()
                var bytesRead: Int
                var total = 0L
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    out.write(buffer, 0, bytesRead)
                    total += bytesRead
                    onProgress?.invoke(total)
                }
                out.toByteArray()
            } ?: return@withContext false

            for (scheme in listOf("https", "http")) {
                try {
                    android.util.Log.d("LocalSendClient", "uploadFile -> $scheme://$ip:$port/api/localsend/v2/upload (size=${bytes.size})")
                    val response = client.post("$scheme://$ip:$port/api/localsend/v2/upload") {
                        url {
                            parameters.append("sessionId", sessionId)
                            parameters.append("fileId", fileId)
                            parameters.append("token", token)
                        }
                        contentType(ContentType.Application.OctetStream)
                        setBody(bytes)
                    }
                    android.util.Log.d("LocalSendClient", "uploadFile response: ${response.status}")
                    val ok = response.status.isSuccess()
                    if (ok) {
                        onProgress?.invoke(bytes.size.toLong())
                        return@withContext true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("LocalSendClient", "uploadFile error ($scheme): ${e.message}", e)
                }
            }
            false
        } catch (e: Exception) {
            android.util.Log.e("LocalSendClient", "uploadFile general error: ${e.message}", e)
            false
        }
    }

    suspend fun cancelSession(ip: String, port: Int, sessionId: String) {
        try {
            client.post("https://$ip:$port/api/localsend/v2/cancel") {
                url { parameters.append("sessionId", sessionId) }
            }
        } catch (_: Exception) {}
    }
}
