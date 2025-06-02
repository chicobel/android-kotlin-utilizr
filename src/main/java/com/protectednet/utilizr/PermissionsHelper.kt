package com.protectednet.utilizr

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionsHelper {

    /**
     * Checks if all permissions are granted
     * @param context The context to check permissions against
     * @param permissions Array of permission strings to check
     * @return true if all permissions are granted, false otherwise
     */
    fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        return hasPermissions(context, permissions.toList())
    }

    /**
     * Checks if all permissions in a collection are granted
     * @param context The context to check permissions against
     * @param permissions Collection of permission strings to check
     * @return true if all permissions are granted, false otherwise
     */
    fun hasPermissions(context: Context, permissions: Collection<String>): Boolean {
        // Permissions always granted below API 23 (Marshmallow)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}