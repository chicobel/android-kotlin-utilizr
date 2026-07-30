package com.protectednet.utilizr.encryption

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreStringCodec(
    private val keyAlias: String,
    private val logTag: String,
    private val prefix: String = DEFAULT_PREFIX,
) {
    companion object {
        const val DEFAULT_PREFIX = "enc:v1:"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val GCM_IV_LENGTH_BYTES = 12
    }

    sealed interface DecodeResult {
        data object Blank : DecodeResult
        data class PlaintextLegacy(val value: String) : DecodeResult
        data class Decrypted(val value: String) : DecodeResult
        data object DecryptionFailed : DecodeResult
    }

    fun encodeForStorage(value: String): String {
        if (value.isBlank() || value.startsWith(prefix)) return value

        val encrypted = encrypt(value) ?: return value
        return prefix + encrypted
    }

    fun decodeFromStorage(storedValue: String): DecodeResult {
        if (storedValue.isBlank()) return DecodeResult.Blank
        if (!storedValue.startsWith(prefix)) return DecodeResult.PlaintextLegacy(storedValue)

        val payload = storedValue.removePrefix(prefix)
        val decrypted = decrypt(payload) ?: return DecodeResult.DecryptionFailed
        return DecodeResult.Decrypted(decrypted)
    }

    private fun encrypt(value: String): String? {
        return runCatching {
            val key = getOrCreateSecretKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val payload = ByteBuffer.allocate(cipher.iv.size + encryptedBytes.size)
                .put(cipher.iv)
                .put(encryptedBytes)
                .array()
            Base64.encodeToString(payload, Base64.NO_WRAP)
        }.onFailure {
            Log.w(logTag, "Failed to encrypt value for storage", it)
        }.getOrNull()
    }

    private fun decrypt(base64Payload: String): String? {
        return runCatching {
            val key = getOrCreateSecretKey() ?: return null
            val payload = Base64.decode(base64Payload, Base64.NO_WRAP)
            if (payload.size <= GCM_IV_LENGTH_BYTES) return null

            val buffer = ByteBuffer.wrap(payload)
            val iv = ByteArray(GCM_IV_LENGTH_BYTES)
            buffer.get(iv)
            val encryptedBytes = ByteArray(buffer.remaining())
            buffer.get(encryptedBytes)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        }.onFailure {
            Log.w(logTag, "Failed to decrypt value from storage", it)
        }.getOrNull()
    }

    private fun getOrCreateSecretKey(): SecretKey? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null

        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}

