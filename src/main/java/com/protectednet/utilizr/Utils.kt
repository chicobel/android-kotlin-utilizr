package com.protectednet.utilizr

import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.util.Log
import java.io.File
import java.net.NetworkInterface
import java.util.Scanner
import kotlin.math.floor

class Utils {

    companion object {
        @SuppressWarnings("deprecation")
        fun fromHtml(html: String?): Spanned {
            if (html == null)
                return SpannableString("")
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(html)
            }
        }

        fun readLinesWithCallback(filePath: String, callback: (Int, String) -> Unit) {
            Scanner(File(filePath), "UTF-8").use { sc ->
                var index = 0
                while (sc.hasNextLine()) {
                    callback(index, sc.nextLine())
                    index++
                }
                // note that Scanner suppresses exceptions
                if (sc.ioException() != null) {
                    throw sc.ioException()
                }
            }
        }

        fun generateRandomPassword(): String {
            val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            var passWord = ""
            for (i in 0..31) {
                passWord += chars[floor(Math.random() * chars.length).toInt()]
            }
            return passWord
        }

        fun hasNetworkInterfaceName(name: String): Boolean {
            return try {
                NetworkInterface.getNetworkInterfaces().asSequence()
                    .filter { it.isUp }
                    .map { it.name }
                    .any { it.contains(name) }
            } catch (ex: Exception) {
                Log.d("isVPNTrulyActive", "Network List not received")
                false
            }
        }

    }

}
