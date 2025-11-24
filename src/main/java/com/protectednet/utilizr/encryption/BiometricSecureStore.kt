package com.protectednet.utilizr.encryption

import android.content.Context
import androidx.fragment.app.FragmentActivity

interface BiometricSecureStore {
    fun isBiometricAvailable(context: Context): Boolean

    suspend fun storeString(
        activity: FragmentActivity,
        id: String,
        value: String,
        title: String = "Authenticate",
        subtitle: String = "Confirm your identity to save data"
    ): Boolean

    suspend fun getString(
        activity: FragmentActivity,
        id: String,
        title: String = "Authenticate",
        subtitle: String = "Confirm your identity to view data"
    ): String?

    suspend fun clearString(id: String)
}