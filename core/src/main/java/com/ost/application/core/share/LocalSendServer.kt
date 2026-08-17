package com.ost.application.core.share

import android.content.Context
import android.os.Build
import android.util.Log
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.netty.Netty
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveText
import io.ktor.utils.io.readAvailable
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LocalSendServer(private val context: Context) {

    private val TAG = "LocalSendServer"
    private var server: io.ktor.server.engine.ApplicationEngine? = null
    private val sessions = ConcurrentHashMap<String, SessionContext>()

    data class SessionContext(val id: String, val request: LocalSendPrepareRequest, val tokens: Map<String, String>)

    var onPrepareUpload: (suspend (LocalSendPrepareRequest) -> Boolean)? = null
    var onTransferProgress: ((String, String, Long) -> Unit)? = null
    var onTransferComplete: ((String, String, File) -> Unit)? = null
    var onDeviceRegistered: ((DiscoveredDevice) -> Unit)? = null

    var actualPort: Int = 53317
        private set
    var fingerprint: String = ""
        private set

    private fun isPortAvailable(port: Int): Boolean {
        return try {
            java.net.ServerSocket(port).use { true }
        } catch (_: Exception) {
            false
        }
    }

    fun start(preferredPort: Int = 53317): Pair<Int, String> {
        val (keyStore, fp) = LocalSendCertificateHelper.getOrGenerateKeyStore()

        var selectedPort = preferredPort
        for (candidate in preferredPort..(preferredPort + 20)) {
            if (isPortAvailable(candidate)) {
                selectedPort = candidate
                break
            }
        }
        actualPort = selectedPort
        fingerprint = fp

        val env = io.ktor.server.engine.applicationEngineEnvironment {
            sslConnector(
                keyStore = keyStore,
                keyAlias = "localsend",
                keyStorePassword = { "localsend".toCharArray() },
                privateKeyPassword = { "localsend".toCharArray() }
            ) {
                this.port = selectedPort
            }
        }

        val nettyServer = embeddedServer(Netty, environment = env)
        installRoutes(nettyServer.application)
        server = nettyServer
        nettyServer.start(wait = false)

        Log.i(TAG, "LocalSend server started on port $selectedPort with fingerprint $fingerprint")
        return Pair(selectedPort, fingerprint)
    }

    fun installRoutes(application: io.ktor.server.application.Application) {
        val gson = com.google.gson.Gson()
        val port = actualPort
        val fp = fingerprint

        application.routing {
            get("/api/localsend/v2/info") {
                val info = LocalSendInfo(
                    alias = Build.MODEL,
                    version = "2.1",
                    deviceModel = Build.MODEL,
                    deviceType = LocalSendDeviceType.MOBILE.value,
                    fingerprint = fp,
                    port = port,
                    protocol = "https",
                    download = false
                )
                call.respondText(gson.toJson(info), ContentType.Application.Json)
            }

            post("/api/localsend/v2/register") {
                try {
                    val bodyText = call.receiveText()
                    val remoteInfo = gson.fromJson(bodyText, LocalSendInfo::class.java)
                    val remoteHost = call.request.local.remoteHost
                    Log.d(TAG, "register request received from ${remoteInfo.alias} ($remoteHost:${remoteInfo.port})")

                    val device = DiscoveredDevice(
                        id = remoteInfo.fingerprint.ifBlank { "$remoteHost:${remoteInfo.port}" },
                        name = remoteInfo.alias,
                        type = remoteInfo.deviceType ?: "desktop",
                        isResolved = true,
                        ipAddress = java.net.InetAddress.getByName(remoteHost),
                        port = remoteInfo.port,
                        isResolving = false
                    )
                    onDeviceRegistered?.invoke(device)

                    val selfInfo = LocalSendInfo(
                        alias = Build.MODEL,
                        version = "2.1",
                        deviceModel = Build.MODEL,
                        deviceType = LocalSendDeviceType.MOBILE.value,
                        fingerprint = fp,
                        port = port,
                        protocol = "https",
                        download = false
                    )
                    call.respondText(gson.toJson(selfInfo), ContentType.Application.Json)
                } catch (e: Exception) {
                    Log.e(TAG, "register error: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Error")
                }
            }

            post("/api/localsend/v2/prepare-upload") {
                try {
                    val bodyText = call.receiveText()
                    val request = gson.fromJson(bodyText, LocalSendPrepareRequest::class.java)
                    Log.d(TAG, "prepare-upload received from ${request.info.alias} (${request.files.size} files)")
                    val accepted = onPrepareUpload?.invoke(request) ?: false
                    if (accepted) {
                        val sessionId = UUID.randomUUID().toString()
                        val tokens = request.files.keys.associateWith { UUID.randomUUID().toString() }
                        sessions[sessionId] = SessionContext(sessionId, request, tokens)
                        Log.d(TAG, "prepare-upload accepted, sessionId=$sessionId")
                        val responseObj = LocalSendPrepareResponse(sessionId, tokens)
                        call.respondText(gson.toJson(responseObj), ContentType.Application.Json)
                    } else {
                        Log.w(TAG, "prepare-upload rejected by user")
                        call.respond(HttpStatusCode.Forbidden, "Rejected")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "prepare-upload error: ${e.message}", e)
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Error")
                }
            }

            post("/api/localsend/v2/upload") {
                val sessionId = call.request.queryParameters["sessionId"]
                val fileId = call.request.queryParameters["fileId"]
                val token = call.request.queryParameters["token"]
                Log.d(TAG, "upload request: sessionId=$sessionId, fileId=$fileId")

                val session = sessions[sessionId]
                if (session == null || session.tokens[fileId] != token) {
                    Log.e(TAG, "upload rejected: invalid session or token")
                    call.respond(HttpStatusCode.Forbidden, "Invalid session or token")
                    return@post
                }

                val fileMeta = session.request.files[fileId]
                if (fileMeta == null) {
                    Log.e(TAG, "upload rejected: fileMeta not found for fileId=$fileId")
                    call.respond(HttpStatusCode.BadRequest, "File info not found")
                    return@post
                }

                try {
                    val ostDir: File = run {
                        val publicDownloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                        val fallbackDir = this@LocalSendServer.context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: this@LocalSendServer.context.filesDir
                        val baseDir = if (publicDownloads != null && publicDownloads.exists() && publicDownloads.canWrite()) publicDownloads else fallbackDir
                        val dir = File(baseDir, Constants.FILES_DIR)
                        if (!dir.exists()) dir.mkdirs()
                        if (dir.exists() && dir.canWrite()) dir else File(this@LocalSendServer.context.filesDir, Constants.FILES_DIR).apply { mkdirs() }
                    }
                    val targetFile = run {
                        var candidate = File(ostDir, fileMeta.fileName)
                        if (candidate.exists()) {
                            val nameNoExt = fileMeta.fileName.substringBeforeLast('.')
                            val ext = fileMeta.fileName.substringAfterLast('.', "")
                            var counter = 1
                            do {
                                val newName = if (ext.isNotEmpty()) "$nameNoExt ($counter).$ext"
                                              else "$nameNoExt ($counter)"
                                candidate = File(ostDir, newName)
                                counter++
                            } while (candidate.exists())
                        }
                        candidate
                    }
                    Log.d(TAG, "uploading ${fileMeta.fileName} (${fileMeta.size} bytes) -> ${targetFile.name}")

                    val channel = call.receiveChannel()
                    withContext(Dispatchers.IO) {
                        FileOutputStream(targetFile).use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesCopied: Long = 0
                            while (!channel.isClosedForRead) {
                                val bytes = channel.readAvailable(buffer, 0, buffer.size)
                                if (bytes == -1) break
                                if (bytes > 0) {
                                    outputStream.write(buffer, 0, bytes)
                                    bytesCopied += bytes
                                    onTransferProgress?.invoke(sessionId ?: "", fileId ?: "", bytesCopied)
                                }
                            }
                        }
                    }
                    Log.d(TAG, "upload complete: ${targetFile.name} (${targetFile.length()} bytes)")
                    onTransferComplete?.invoke(sessionId ?: "", fileId ?: "", targetFile)
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    Log.e(TAG, "upload error: ${e.message}", e)
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
                }
            }

            post("/api/localsend/v2/cancel") {
                val sessionId = call.request.queryParameters["sessionId"]
                sessions.remove(sessionId)
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    fun stop() {
        try {
            server?.stop(1000, 2000)
            server = null
            sessions.clear()
            Log.i(TAG, "LocalSend server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server: ${e.message}", e)
        }
    }
}
