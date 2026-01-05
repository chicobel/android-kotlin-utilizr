package com.protectednet.utilizr

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.UrlQuerySanitizer
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.annotation.AnimRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.protectednet.utilizr.AppUtils.getDefaultBrowserPackageName
import java.net.URI

/**
 * Utility class for handling URLs and web-related operations.
 * This class provides common functionality for opening URLs, sharing, copying to clipboard,
 * and URL sanitization that can be used across different Android applications.
 */
object UrlUtil {
    private val TAG = UrlUtil::class.java.simpleName

    data class SanitizedOutput(val sanitizedDomain: String, val sanitizedUrl: String)

    /**
     * Opens a URL in the device's default browser or app.
     * @param context The context to use for starting the activity
     * @param url The URL to open
     * @param onError Optional callback for handling errors
     */
    fun openUrl(
        context: Context,
        url: String,
        onError: (() -> Unit)? = null
    ) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = url.toUri()
            }

            context.startActivity(
                Intent.createChooser(intent, null).apply {
                    addTaskFlag(context)
                }
            )
        } catch (e: ActivityNotFoundException) {
            if (onError != null) {
                onError()
            } else {
                copyAndNotify(context, url)
            }
        }
    }

    /**
     * Shares a URL using the system share dialog.
     * @param context The context to use for sharing
     * @param url The URL to share
     * @param onError Callback for handling errors
     */
    fun shareUrl(
        context: Context,
        url: String,
        onError: () -> Unit = { copyAndNotify(context, url) }
    ) {
        try {
            ShareCompat.IntentBuilder(context)
                .setType("text/plain")
                .setText(url)
                .startChooser()
        } catch (e: ActivityNotFoundException) {
            onError()
        }
    }

    /**
     * Opens a URL in the device's default browser.
     * @param context The context to use for starting the activity
     * @param url The URL to open
     */
    fun openUrlInBrowser(context: Context, url: String) {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (context !is Activity)
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ContextCompat.startActivity(context, browserIntent, null)
        } catch (activityNotFoundException: ActivityNotFoundException) {
            Log.d(TAG, "Unable to open url ${activityNotFoundException.message}")
        }
    }

    /**
     * Opens a URL in a custom tab with animations.
     * Falls back to standard browser if Custom Tabs are not available.
     * @param context The context to use for starting the activity
     * @param url The URL to open
     * @param iconResources The icon to use for the close button
     * @param startAnimEnterResId Animation resource ID for entering
     * @param startAnimExitResId Animation resource ID for exiting
     * @param exitAnimEnterResId Animation resource ID for entering when closing
     * @param exitAnimExitResId Animation resource ID for exiting when closing
     */
    fun openUrlInApp(
        context: Context,
        url: String,
        iconResources: Bitmap?,
        @AnimRes startAnimEnterResId: Int,
        @AnimRes startAnimExitResId: Int,
        @AnimRes exitAnimEnterResId: Int = android.R.anim.slide_in_left,
        @AnimRes exitAnimExitResId: Int = android.R.anim.slide_out_right
    ) {
        try {
            val builder = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .setStartAnimations(context, startAnimEnterResId, startAnimExitResId)
                .setExitAnimations(context, exitAnimEnterResId, exitAnimExitResId)

            // Use custom icon if provided, otherwise use default
            iconResources?.let { builder.setCloseButtonIcon(it) }

            val customTabsIntent = builder.build()

            val packageName = getDefaultBrowserPackageName(context)
            if (packageName != null) {
                customTabsIntent.intent.setPackage(packageName)
            }

            customTabsIntent.launchUrl(context, url.toUri())
        } catch (e: ActivityNotFoundException) {
            // Custom Tabs not available, fallback to standard browser
            Log.d(TAG, "Custom Tabs not available, falling back to standard browser: ${e.message}")
            openUrlInBrowser(context, url)
        } catch (e: Exception) {
            // Any other error, fallback to standard browser
            Log.e(TAG, "Error opening Custom Tab, falling back to standard browser: ${e.message}", e)
            openUrlInBrowser(context, url)
        }
    }

    /**
     * Opens the Play Store page for a specific app.
     * @param context The context to use for starting the activity
     * @param packageName The package name of the app
     * @param onError Callback for handling errors
     */
    fun openMarketUrl(context: Context, packageName: String, onError: () -> Unit = {}) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    data = "market://details?id=$packageName".toUri()
                }
            )
        } catch (e: ActivityNotFoundException) {
            openUrl(
                context,
                "https://play.google.com/store/apps/details?id=$packageName",
                onError
            )
        }
    }

    /**
     * Copies text to the clipboard.
     * @param context The context to use for clipboard operations
     * @param text The text to copy
     * @param label The label for the clipboard entry
     */
    fun copyToClipboard(context: Context, text: String, label: String = "text") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
    }

    private fun copyAndNotify(context: Context, text: String) {
        copyToClipboard(context, text)
        Toast.makeText(
            context,
            "Could not open URL - we have copied it to your clipboard instead",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun Intent.addTaskFlag(context: Context) {
        if (context == context.applicationContext) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Sanitizes a URL and returns both the sanitized domain and URL.
     * @param inputUrl The URL to sanitize
     * @return A [SanitizedOutput] containing the sanitized domain and URL
     */
    fun sanitizeUrl(inputUrl: String): SanitizedOutput {
        try {
            var str = UrlQuerySanitizer.getAllButNulLegal().sanitize(inputUrl)

            // Find the sanitized domain name. val sanitizedDomain = url?.host?.split('/')?.get(0) ?:"" This approach was returning a blank string for strings like "lowtrust.webshieldtest.com"
            var outputDomain = str
            if (!outputDomain.startsWith("http://", true)
                && !outputDomain.startsWith("https://", true)
            ) {
                outputDomain = "http://$outputDomain"
            }
            outputDomain = URI(outputDomain).host // If the url does not start with http:// or https://, this goes blank and hence the above treatment.

            // Some browsers like Chrome strip out the www part from the address bar and this was causing the blockpage to appear twice when it was loaded before the user pressed enter
            outputDomain = outputDomain.replace("www.", "", true)

            // Find the sanitised URL with the query parameters sorted in ascending order alphabetically. Also remove http://, https:// and www from the url.
            // TODO this method expects an encoded string but is being given an unencoded one
            val url = str.toUri()
            val queries = url.queryParameterNames.sorted()
            var sortedQueries = ""
            if (queries.isNotEmpty()) {
                for (i in queries) {
                    sortedQueries += "$i=${url.getQueryParameter(i)}&"
                }
                sortedQueries = sortedQueries.removeSuffix("&")
            }
            if (str.contains('?'))
                str = str.split('?').first() + '?' + sortedQueries
            if (str.startsWith("http://", true))
                str = str.replace("http://", "", true)
            if (str.startsWith("https://"))
                str = str.replace("https://", "", true)
            if (str.startsWith("www."))
                str = str.replace("www.", "", true)
            val outputUrl = str.removeSuffix("/")

            return SanitizedOutput(outputDomain, outputUrl)

        } catch (e: Exception) {
            Log.d(TAG, "[${e.message}] - Problem sanitizing $inputUrl")
        }

        return SanitizedOutput(inputUrl, inputUrl)
    }

    /**
     * Checks if a string is a valid URL.
     * @param textContents The string to check
     * @return true if the string is a valid URL, false otherwise
     */
    fun isValidUrl(textContents: String): Boolean {
        return Patterns.WEB_URL.matcher(textContents).matches()
    }
} 