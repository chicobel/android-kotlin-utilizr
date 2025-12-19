package com.protectednet.utilizr.encryption

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.FragmentActivity
import com.protectednet.utilizr.GetText.L
import com.protectednet.utilizr.encryption.BiometricsUtil.KEY_ALIAS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume

object BiometricsUtil {

    private const val TAG = "BiometricsUtil"
    
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
     * @return Cipher if authentication succeeded, null if cancelled or failed
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
            .setNegativeButtonText(L.t("Cancel"))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (!cont.isActive) return
                    cont.resume(result.cryptoObject?.cipher)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (!cont.isActive) return
                    
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED -> {
                            // User cancellation, no action needed
                        }
                        
                        BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                            Log.w(TAG, "No biometrics enrolled: $errString")
                        }
                        
                        BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                            Log.w(TAG, "Biometric hardware unavailable: $errString")
                        }
                        
                        BiometricPrompt.ERROR_LOCKOUT -> {
                            Log.w(TAG, "Biometric lockout (temporary): $errString")
                        }
                        
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            Log.e(TAG, "Biometric lockout (permanent): $errString")
                        }
                        
                        BiometricPrompt.ERROR_HW_NOT_PRESENT -> {
                            Log.w(TAG, "No biometric hardware present: $errString")
                        }
                        
                        BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> {
                            Log.w(TAG, "Unable to process biometric: $errString")
                        }
                        
                        BiometricPrompt.ERROR_CANCELED -> {
                            Log.d(TAG, "Biometric authentication canceled by system: $errString")
                        }
                        
                        else -> {
                            Log.w(TAG, "Biometric authentication error: code=$errorCode, message=$errString")
                        }
                    }
                    
                    cont.resume(null)
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
            // TODO: decide whether or not to enroll this settings?
//            .apply {
//                // Invalidate key if biometrics are changed, user will need to re-setup
//                // This is more secure and prevents issues with changed biometrics
//                // Ref from 1Password: https://support.1password.com/android-biometric-unlock-security/
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
//                    setInvalidatedByBiometricEnrollment(true)
//            }
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

