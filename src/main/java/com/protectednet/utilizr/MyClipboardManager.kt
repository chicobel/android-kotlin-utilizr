package com.protectednet.utilizr

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.os.Build
import android.content.ClipboardManager
import android.util.Log
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder

class MyClipboardManager {

    @SuppressLint("NewApi")
    fun writeToClipboard(context: Context, data: String){
        val sdk = Build.VERSION.SDK_INT
        val clipboard = if (sdk < Build.VERSION_CODES.HONEYCOMB) {
             context
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        } else {
           context
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        }
        val clip = ClipData.newPlainText("label", data)
        clipboard.setPrimaryClip(clip)
    }
    @SuppressLint("NewApi")
    fun readFromClipboard(context: Context): String {
        val sdk = Build.VERSION.SDK_INT
        if (sdk < Build.VERSION_CODES.HONEYCOMB) {
            val clipboard = context
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            return clipboard.text.toString()
        } else {
            val clipboard: ClipboardManager = context
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            // Gets a content resolver instance
            val cr: ContentResolver = context.contentResolver

            // Gets the clipboard data from the clipboard
            val clip: ClipData? = clipboard.primaryClip
            if (clip != null) {
                var text: String? = null
                val title: String? = null

                // Gets the first item from the clipboard data
                val item: ClipData.Item = clip.getItemAt(0)

                // Tries to get the item's contents as a URI pointing to a note
                val uri = item.uri

                // If the contents of the clipboard wasn't a reference to a
                // note, then
                // this converts whatever it is to text.
                if (text == null) {
                    text = coerceToText(context, item).toString()
                }
                return text
            }
        }
        return ""
    }

    @SuppressLint("NewApi")
    fun coerceToText(context: Context, item: ClipData.Item): CharSequence {
        // If this Item has an explicit textual value, simply return that.
        val text: CharSequence = item.getText()
        if (text != null) {
            return text
        }

        // If this Item has a URI value, try using that.
        val uri = item.getUri()
        if (uri != null) {

            // First see if the URI can be opened as a plain text stream
            // (of any sub-type). If so, this is the best textual
            // representation for it.
            var stream: FileInputStream? = null
            try {
                // Ask for a stream of the desired type.
                val descr: AssetFileDescriptor? = context.getContentResolver()
                    .openTypedAssetFileDescriptor(uri, "text/*", null)
                stream = descr?.createInputStream()
                val reader = InputStreamReader(
                    stream,
                    "UTF-8"
                )

                // Got it... copy the stream into a local string and return it.
                val builder = StringBuilder(128)
                val buffer = CharArray(8192)
                var len: Int
                while (reader.read(buffer).also { len = it } > 0) {
                    builder.append(buffer, 0, len)
                }
                return builder.toString()
            } catch (e: FileNotFoundException) {
                // Unable to open content URI as text... not really an
                // error, just something to ignore.
            } catch (e: IOException) {
                // Something bad has happened.
                Log.w("ClippedData", "Failure loading text", e)
                return e.toString()
            } finally {
                if (stream != null) {
                    try {
                        stream.close()
                    } catch (e: IOException) {
                    }
                }
            }

            // If we couldn't open the URI as a stream, then the URI itself
            // probably serves fairly well as a textual representation.
            return uri.toString()
        }

        // Finally, if all we have is an Intent, then we can just turn that
        // into text. Not the most user-friendly thing, but it's something.
        val intent = item.getIntent()
        return if (intent != null) {
            intent.toUri(Intent.URI_INTENT_SCHEME)
        } else ""

        // Shouldn't get here, but just in case...
    }

    fun readPasteData(c: Context): String {
        val sdk = Build.VERSION.SDK_INT
        val clipboard = if (sdk < Build.VERSION_CODES.HONEYCOMB) {
            c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        } else c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        var pasteData: String = ""
        // Gets the ID of the "paste" menu item

        // If the clipboard doesn't contain data, disable the paste menu item.
        // If it does contain data, decide if you can handle the data.
        val hasdata = when {
            !clipboard.hasPrimaryClip() -> {
                false
            }
            (clipboard.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN) == false) -> {
                // This disables the paste menu item, since the clipboard has data but it is not plain text
                false
            }
            else -> {
                // This enables the paste menu item, since the clipboard contains plain text.
                true
            }
        }
        if(hasdata)
            pasteData = clipboard.primaryClip?.getItemAt(0)?.text.toString()
        return pasteData
    }
}