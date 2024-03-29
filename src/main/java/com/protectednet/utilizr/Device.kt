package com.protectednet.utilizr

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Fetches the personalised device name using various methods, falling back to [Build.MODEL]
 */
@SuppressLint("MissingPermission")
fun Context.getPersonalisedDeviceName(): String {
    // Use a sequence rather than a list so these names are only evaluated if needed and we can stop early
    val names = sequence {
        // Crashes if trying to access bluetooth_name when targetSdkVersion is 32 or higher:
        // java.lang.SecurityException: Settings key: <bluetooth_name> is only readable to apps with
        // targetSdkVersion lower than or equal to: 31
        // So check the targetSdk rather than Build.VERSION.SDK_INT
        if (applicationContext.applicationInfo.targetSdkVersion <= 31) {
            yield(Settings.System.getString(contentResolver, "bluetooth_name"))
            yield(Settings.Secure.getString(contentResolver, "bluetooth_name"))
        }

        if (Build.VERSION.SDK_INT >= 17) {
            yield(Settings.Global.getString(contentResolver, "device_name"))
        }

        /*
        On api 31+ this requires BLUETOOTH_CONNECT which is a dangerous permission, meaning we
        would have to specifically ask for it and seems a bit extra just to get the device name
        On versions 30 and below we can get it with just the BLUETOOTH permission which is a normal,
        permission so only need it in the manifest
        */
        if (Build.VERSION.SDK_INT <= 30 && checkPermission(
                Manifest.permission.BLUETOOTH,
                Process.myPid(),
                Process.myUid()
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT <= 18) {
                @Suppress("DEPRECATION")
                yield(BluetoothAdapter.getDefaultAdapter().name)
            } else {
                val bm = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                yield(bm?.adapter?.name)
            }
        }
        yield(Settings.System.getString(contentResolver, "device_name"))
        yield(Settings.Secure.getString(contentResolver, "lock_screen_owner_info"))
    }

    return names.firstOrNull { !it.isNullOrBlank() } ?: Build.MODEL
}
