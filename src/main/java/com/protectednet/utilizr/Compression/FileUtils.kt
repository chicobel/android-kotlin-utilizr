package com.protectednet.utilizr.Compression

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.zip.Inflater
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object FileUtils {
    fun decompressFile(filePath: String): File? {
        try {
            val content = File(filePath).readBytes()
            val inflater = Inflater()
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)

            inflater.setInput(content)

            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                outputStream.write(buffer, 0, count)
            }
            outputStream.close()
            val outFile = File(filePath.replace(".zlib", ""))
            outputStream.writeTo(FileOutputStream(outFile))
            return outFile
        } catch (e: Exception) {
            Log.e("FileUtils-Decompress", e.message ?: "")
        }
        return null
    }
}