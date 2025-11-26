package com.protectednet.utilizr

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.browser.customtabs.CustomTabsService
import com.protectednet.utilizr.UrlUtil.openMarketUrl
import java.security.MessageDigest

/**
 * Represents some basic application info
 */
data class BasicApplicationInfo(
    /**
     * The application package name
     */
    val packageName: String,
    /**
     * The application's pretty display name
     */
    val displayName: String,
    /**
     * The application's icon - if there isn't one, i.e the icon is the default one, this will be null
     */
    val icon: Drawable?,
    /**
     * The backing application info
     */
    val appInfo: ApplicationInfo
)

val ApplicationInfo.isSystemApp: Boolean
    get() = (flags and ApplicationInfo.FLAG_SYSTEM) != 0

val BasicApplicationInfo.isSystemApp: Boolean
    get() = appInfo.isSystemApp

object AppUtils {

    /**
     * Returns a list of all launchable apps - apps that have [Intent.ACTION_MAIN] and [Intent.CATEGORY_LAUNCHER]
     *
     * The list will not include the current app that [context] is a part of
     */
    fun getLaunchableApps(context: Context, excludeSystemApps: Boolean = true): List<BasicApplicationInfo> {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
//        intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)

        val packageManager = context.applicationContext.packageManager

        val allApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            packageManager.queryIntentActivities(intent, 0)
        }

        return allApps
            .asSequence() // use a sequence to improve performance as there could be a very large number of activities
            .mapNotNull { it.activityInfo } // not launchable
            .filter { it.packageName != context.applicationContext.packageName } // don't include our app
            .distinctBy { it.packageName } // 1 app can have many activities, so only include unique apps
            .mapNotNull { packageManager.resolveAppInfo(it.packageName) }
            .filterIf(excludeSystemApps) {
                filter { it.appInfo.isSystemApp.not() }
            }
            .sortedBy { it.displayName }
            .toList()
    }

    /**
     * Resolves an app's info from its package name
     *
     * If the package is not found, this will return null rather than throwing a [NameNotFoundException][PackageManager.NameNotFoundException]
     */
    fun resolveAppInfo(context: Context, packageName: String): BasicApplicationInfo? {
        val packageManager = context.applicationContext.packageManager

        val appInfo =
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(
                        packageName,
                        PackageManager.ApplicationInfoFlags.of(0)
                    )
                } else {
                    packageManager.getApplicationInfo(packageName, 0)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                null
            } ?: return null

        return packageManager.resolveAppInfo(appInfo)
    }

    private fun PackageManager.resolveAppInfo(packageName: String): BasicApplicationInfo? {
        val appInfo =
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getApplicationInfo(
                        packageName,
                        PackageManager.ApplicationInfoFlags.of(0)
                    )
                } else {
                    getApplicationInfo(packageName, 0)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                null
            } ?: return null

        return resolveAppInfo(appInfo)
    }

    private fun PackageManager.resolveAppInfo(appInfo: ApplicationInfo): BasicApplicationInfo {
        var icon: Drawable? = appInfo.loadIcon(this)
        val defaultIcon = defaultActivityIcon

        // loadIcon returns the default icon if one is not set, however we want to be able to handle this case
        // and maybe replace with our own placeholder etc
        // so if the returned icon is the same as the default icon, return a null icon
        if (icon is BitmapDrawable && defaultIcon is BitmapDrawable) {
            if (icon.bitmap == defaultIcon.bitmap) {
                icon = null
            }
        }

        val name = appInfo.loadLabel(this).toString()

        return BasicApplicationInfo(appInfo.packageName, name, icon, appInfo)
    }

    fun getDefaultBrowserPackageName(context: Context): String? {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
        val flags = PackageManager.MATCH_DEFAULT_ONLY.toLong()
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.resolveActivity(browserIntent, PackageManager.ResolveInfoFlags.of(flags))
        } else {
            context.packageManager.resolveActivity(browserIntent, flags.toInt())
        }
        return resolveInfo?.activityInfo?.packageName
    }

    fun getCustomTabPackages(context: Context): List<ResolveInfo> {
        // Get default VIEW intent handler.
        val activityIntent = Intent()
            .setAction(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.fromParts("http", "", null))
        // Get all apps that can handle VIEW intents.
        val resolvedActivityList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                activityIntent, PackageManager.ResolveInfoFlags.of(
                    PackageManager.MATCH_ALL.toLong()
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Without this all browsers were not being returned. Refer: https://stackoverflow.com/a/57229289
            context.packageManager.queryIntentActivities(activityIntent, PackageManager.MATCH_ALL)
        } else {
            context.packageManager.queryIntentActivities(
                activityIntent,
                0
            ) // On why flags is set to 0 rather than a constant: https://stackoverflow.com/questions/9623079/why-does-the-flag-specified-in-queryintentactivities-method-is-set-to-zero
        }

        val pckgsSupportingCustomTabs = ArrayList<ResolveInfo>()
        for (resolveInfo in resolvedActivityList) {
            val serviceIntent = Intent()
            serviceIntent.action = CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION
            serviceIntent.setPackage(resolveInfo.activityInfo.packageName)
            // Check if this package also resolves the Custom Tabs service.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.packageManager.resolveService(
                        serviceIntent,
                        PackageManager.ResolveInfoFlags.of(0L)
                    ) != null
                ) {
                    pckgsSupportingCustomTabs.add(resolveInfo)
                }
            } else {
                if (context.packageManager.resolveService(serviceIntent, 0) != null) {
                    pckgsSupportingCustomTabs.add(resolveInfo)
                }
            }
        }
        return pckgsSupportingCustomTabs
    }

    fun isAppInstalled(context: Context, packageName: String?): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName!!, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openAppOrPlayStore(context: Context, packageName: String) {
        val intent =
            context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            openMarketUrl(context, packageName)
        }
    }


    fun getAppSignersFingerprint(context: Context,algorithm: String = "SHA-256"): List<String> {
        val packageName = context.packageName
        val packageManager = context.applicationContext.packageManager

        val pkgInfo = packageManager.getPackageInfo(
            packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )

        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.signatures
        }
        val mutableList = mutableListOf<String>()
        // Loop through each signature and compute fingerprint
        if (signatures != null) {
            for (sig in signatures) {
                val digest = MessageDigest.getInstance(algorithm)
                val hashBytes = digest.digest(sig.toByteArray())
                // Convert to Base64
                val base64Fingerprint = Base64.encodeToString(hashBytes, Base64.NO_WRAP)

                Log.i("AppFingerprint", "$algorithm (Base64): $base64Fingerprint")
                mutableList.add(base64Fingerprint)
            }
        }

        return mutableList
    }

}
