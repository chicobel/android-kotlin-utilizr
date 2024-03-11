package com.protectednet.utilizr.encryption

import android.util.Base64
import android.util.Log
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.experimental.and

object EncryptionUtils {

    fun toSHA1(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            val messageDigest = md.digest(input.toByteArray())
            val no = BigInteger(1, messageDigest)
            var hashtext = no.toString(16)
            while (hashtext.length < 32) {
                hashtext = "0$hashtext"
            }
            hashtext
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }

    fun toSHA256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val messageDigest = md.digest(input.toByteArray())
            val no = BigInteger(1, messageDigest)
            var hashtext = no.toString(16)
            while (hashtext.length < 32) {
                hashtext = "0$hashtext"
            }
            hashtext
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }

    fun toSHA512(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-512")
            val messageDigest = md.digest(input.toByteArray())
            val no = BigInteger(1, messageDigest)
            var hashtext = no.toString(16)
            while (hashtext.length < 32) {
                hashtext = "0$hashtext"
            }
            hashtext
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }

    fun decodeJwt(jwt: String): String {
        val result = StringBuilder()
        val parts = jwt.split(".")
        try {
            var index = 0
            for (part in parts) {
                if (index >= 2) break
                index++
                val decodedBytes: ByteArray =
                    Base64.decode(part.toByteArray(charset("UTF-8")), Base64.URL_SAFE)
                if(index == 1) continue //only interested in the payload bit
                result.append(String(decodedBytes, charset("UTF-8")))
            }
        } catch (e: Exception) {
            Log.e("Utils","Couldn't decode jwt", e)
        }
        return  result.toString()
    }

}