package com.protectednet.utilizr.encryption

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import kotlin.coroutines.resume

object BiometricsUtil {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "biometric_master_key"

    private const val AES_MODE = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128

    /**
    * Checks if biometric authentication is available on the device
    * */
    fun isBiometricAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val res = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return res == BiometricManager.BIOMETRIC_SUCCESS
    }

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun storeString(
        activity: FragmentActivity,
        dataStore: DataStore<Preferences>,
        id: String,
        value: String,
        title: String = "Authenticate",
        subtitle: String = "Confirm your identity to save data"
    ): Boolean {
        ensureKeyExists()
        val cipher = createCipherForEncryption()

        // Biometric step
        val authenticatedCipher = authenticateCipher(
            activity = activity,
            cipher = cipher,
            title = title,
            subtitle = subtitle
        ) ?: return false

        // Encrypt
        val encryptedBytes = authenticatedCipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val iv = authenticatedCipher.iv

        // Store in DataStore
        saveEncrypted(dataStore, id, encryptedBytes, iv)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.M)
    suspend fun getString(
        activity: FragmentActivity,
        dataStore: DataStore<Preferences>,
        id: String,
        title: String = "Authenticate",
        subtitle: String = "Confirm your identity to get data"
    ): String? {
        // Load from DataStore first
        val stored = loadEncrypted(dataStore, id) ?: return null
        val (encryptedBytes, iv) = stored

        ensureKeyExists()
        val cipher = createCipherForDecryption(iv)

        // Biometric step
        val authenticatedCipher = authenticateCipher(
            activity = activity,
            cipher = cipher,
            title = title,
            subtitle = subtitle
        ) ?: return null

        // Decrypt
        val decryptedBytes = authenticatedCipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
    * Clears the encrypted data from DataStore
    *  - Note: This will not remove the key from the keystore
    * */
    suspend fun clearString(
        dataStore: DataStore<Preferences>,
        id: String
    ) {
        dataStore.edit { prefs ->
            prefs.remove(valueKey(id))
            prefs.remove(ivKey(id))
        }
    }

    /**
     * Authenticates the user with biometric prompt and returns the authenticated cipher
     */
    private suspend fun authenticateCipher(
        activity: FragmentActivity,
        cipher: Cipher,
        title: String,
        subtitle: String
    ): Cipher? = suspendCancellableCoroutine { cont ->
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authenticatedCipher = result.cryptoObject?.cipher
                    cont.resume(authenticatedCipher)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (cont.isActive) cont.resume(null)
                }

                override fun onAuthenticationFailed() {
                    // User can retry, prompt stays open
                }
            }
        )

        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))

        cont.invokeOnCancellation {
            prompt.cancelAuthentication()
        }
    }


    //region KeyStore helpers

    /**
     * Ensures that a key with [KEY_ALIAS] exists in the Android Keystore
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun ensureKeyExists() {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!ks.containsAlias(KEY_ALIAS)) createSecretKey()
    }

    /**
     * Creates a new key with [KEY_ALIAS] in the Android Keystore
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun createSecretKey() {
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            //uncomment if desired, not sure you'd would want to bug users
//            .setInvalidatedByBiometricEnrollment(true)
            .build()

        generator.init(spec)
        generator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun createCipherForEncryption(): Cipher {
        return Cipher.getInstance(AES_MODE).apply {
            init(Cipher.ENCRYPT_MODE, getSecretKey())
        }
    }

    private fun createCipherForDecryption(iv: ByteArray): Cipher {
        return Cipher.getInstance(AES_MODE).apply {
            init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(TAG_LENGTH, iv))
        }
    }
    //endregion

    //region DataStore helpers

    private fun valueKey(id: String) = stringPreferencesKey("${id}_value")
    private fun ivKey(id: String) = stringPreferencesKey("${id}_iv")

    private suspend fun saveEncrypted(
        dataStore: DataStore<Preferences>,
        id: String,
        bytes: ByteArray,
        iv: ByteArray
    ) {
        val encValue = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val encIv = Base64.encodeToString(iv, Base64.NO_WRAP)

        dataStore.edit { prefs ->
            prefs[valueKey(id)] = encValue
            prefs[ivKey(id)] = encIv
        }
    }

    private suspend fun loadEncrypted(
        dataStore: DataStore<Preferences>,
        id: String
    ): Pair<ByteArray, ByteArray>? {
        val prefs = dataStore.data.first()

        val v = prefs[valueKey(id)] ?: return null
        val iv = prefs[ivKey(id)] ?: return null

        val valueBytes = Base64.decode(v, Base64.NO_WRAP)
        val ivBytes = Base64.decode(iv, Base64.NO_WRAP)

        return valueBytes to ivBytes
    }

    //endregion
}

