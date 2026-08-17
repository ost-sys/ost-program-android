package com.ost.application.settings
import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import com.ost.application.core.settings.sync.SettingsSyncClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class GithubTokenRepository(context: Context, private val scope: CoroutineScope) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val syncClient = SettingsSyncClient(context)
    private val _token = MutableStateFlow(loadToken())
    val token: StateFlow<String> = _token.asStateFlow()
    fun setToken(value: String) {
        _token.value = value
        val encrypted = encrypt(value)
        prefs.edit {
            if (encrypted != null) putString(KEY_TOKEN_ENCRYPTED, encrypted) else remove(KEY_TOKEN_ENCRYPTED)
            remove(KEY_TOKEN_LEGACY)
        }
        pushPresence()
    }
    fun clearToken() {
        _token.value = ""
        prefs.edit {
            remove(KEY_TOKEN_ENCRYPTED)
            remove(KEY_TOKEN_LEGACY)
        }
        pushPresence()
    }
    fun pushPresence() {
        scope.launch { syncClient.pushGithubTokenPresence(_token.value.isNotBlank()) }
    }
    private fun loadToken(): String {
        prefs.getString(KEY_TOKEN_LEGACY, null)?.let { legacy ->
            val encrypted = encrypt(legacy)
            prefs.edit {
                if (encrypted != null) putString(KEY_TOKEN_ENCRYPTED, encrypted)
                remove(KEY_TOKEN_LEGACY)
            }
            return legacy
        }
        val stored = prefs.getString(KEY_TOKEN_ENCRYPTED, null) ?: return ""
        return decrypt(stored) ?: ""
    }
    private fun encrypt(plain: String): String? {
        if (plain.isEmpty()) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(cipher.iv + cipherText, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt token", e)
            null
        }
    }
    private fun decrypt(stored: String): String? {
        return try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, GCM_IV_LENGTH)
            val cipherText = bytes.copyOfRange(GCM_IV_LENGTH, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt token", e)
            null
        }
    }
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }
    companion object {
        private const val TAG = "GithubTokenRepository"
        private const val PREFS_NAME = "github_prefs"
        private const val KEY_TOKEN_LEGACY = "token"
        private const val KEY_TOKEN_ENCRYPTED = "token_enc"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "ost_github_token_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
