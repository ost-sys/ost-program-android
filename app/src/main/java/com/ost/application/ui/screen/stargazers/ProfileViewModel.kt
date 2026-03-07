package com.ost.application.ui.screen.stargazers

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.ost.application.data.model.GitHubUser
import com.ost.application.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel : ViewModel() {

    private val _user = MutableStateFlow<GitHubUser?>(null)
    val user = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _qrCodeBitmap = MutableStateFlow<Bitmap?>(null)
    val qrCodeBitmap = _qrCodeBitmap.asStateFlow()

    fun loadProfile(username: String, token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val tokenHeader = if (token.startsWith("token ")) token else "token $token"
                val fullUser = RetrofitClient.api.getUserDetails(
                    username = username,
                    token = tokenHeader
                )
                _user.value = fullUser
                generateQrCode(fullUser.htmlUrl)
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun generateQrCode(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                try {
                    val writer = QRCodeWriter()
                    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
                    val width = bitMatrix.width
                    val height = bitMatrix.height
                    val bmp = createBitmap(width, height, Bitmap.Config.RGB_565)
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            bmp[x, y] = if (bitMatrix.get(
                                    x,
                                    y
                                )
                            ) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                        }
                    }
                    bmp
                } catch (_: Exception) {
                    null
                }
            }
            _qrCodeBitmap.value = bitmap
        }
    }
}