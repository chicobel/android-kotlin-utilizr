package com.protectednet.utilizr

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.MeasureSpec.*
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import java.math.BigInteger
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.isWifiOn():Boolean{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) isWifiOnNewer(this) else isWifiOnOld(this)
}

@TargetApi(16)
private fun isWifiOnOld(context: Context):Boolean{
    val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
    return if (connManager is ConnectivityManager) {
        val networkInfo = connManager.activeNetworkInfo
        networkInfo!=null && networkInfo.isConnected && networkInfo.type == ConnectivityManager.TYPE_WIFI
    } else false
}

@TargetApi(23)
private fun isWifiOnNewer(context: Context):Boolean{
    val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
    return if (connManager is ConnectivityManager) {
        val networkInfo = connManager.activeNetwork
        val capabilities = connManager.getNetworkCapabilities(connManager.activeNetwork)
        networkInfo!=null && capabilities !=  null && capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_WIFI)
    } else false
}

fun Drawable.toBitmap(): Bitmap? {
    try {
        if (this is BitmapDrawable) {
            val bitmapDrawable: BitmapDrawable = this
            if (bitmapDrawable.bitmap != null) {
                return bitmapDrawable.bitmap
            }
        }
        val bitmap: Bitmap = if (this.intrinsicWidth <= 0 || this.intrinsicHeight <= 0) {
            Bitmap.createBitmap(
                1,
                1,
                Bitmap.Config.ARGB_8888
            ) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(
                this.intrinsicWidth,
                this.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }
        val canvas = Canvas(bitmap)
        this.setBounds(0, 0, canvas.width, canvas.height)
        this.draw(canvas)
        return bitmap
    } catch (e: Exception) {

    }
    return null
}

fun Bitmap.getCroppedRoundedBitmap(): Bitmap? {
    val output = Bitmap.createBitmap(
        this.width,
        this.height, Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(output)
    val color = -0xbdbdbe
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rect = Rect(0, 0, this.width, this.height)
    canvas.drawARGB(0, 0, 0, 0)
    paint.color = color
    canvas.drawCircle(
        this.width / 2f, this.height / 2f,
        this.width / 2f, paint
    )
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, rect, rect, paint)
//    this.recycle()
    return output
}

fun Bitmap.toGrayScale():Bitmap{
    val height: Int = this.height
    val width: Int = this.width

    val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmpGrayscale)
    val paint = Paint()
    val cm = ColorMatrix()
    cm.setSaturation(0f)
    val f = ColorMatrixColorFilter(cm)
    paint.colorFilter = f
    c.drawBitmap(this, 0f, 0f, paint)
//    this.recycle()
    return bmpGrayscale
}



fun Context.openUrlInBrowser(url:String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (this !is Activity)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val c = intent.resolveActivityInfo(this.packageManager, intent.flags)
    if (c != null && c.exported)
        this.startActivity(intent)
}

fun Resources.decodeSampledBitmapFromResource(
    resId: Int,
    reqWidth: Int,
    reqHeight: Int
): Bitmap {
    val res=this
    // First decode with inJustDecodeBounds=true to check dimensions
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeResource(res, resId, this)

        // Calculate inSampleSize
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

        // Decode bitmap with inSampleSize set
        inJustDecodeBounds = false

        BitmapFactory.decodeResource(res, resId, this)
    }
}
fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    // Raw height and width of image
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
        // height and width larger than the requested height and width.
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

fun String.toHashSHA256(): String {
    val bytes = this.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("", { str, it -> str + "%02x".format(it) })
}

/*
Get date string with specified [format]
* */
fun Date.toString(format:String):String{
    val dateFormat = SimpleDateFormat(format, Locale.getDefault())
    return  dateFormat.format(this)
}

fun Date.addMilliseconds(milliseconds:Int):Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.MILLISECOND, milliseconds)
    return calendar.time
}
fun Date.addHours(hours:Int):Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.HOUR_OF_DAY, hours)
    return calendar.time
}
fun Date.addMinutes(minutes:Int):Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.MINUTE, minutes)
    return calendar.time
}
fun Long.toSizeSuffix(): String {
    var size = this
    var suffix: String? = null

    if (size >= 1024) {
        suffix = "KB"
        size /= 1024
        if (size >= 1024) {
            suffix = "MB"
            size /= 1024
        }
    }

    val resultBuffer = StringBuilder(java.lang.Long.toString(size))

    var commaOffset = resultBuffer.length - 3
    while (commaOffset > 0) {
        resultBuffer.insert(commaOffset, ',')
        commaOffset -= 3
    }

    if (suffix != null) resultBuffer.append(suffix)
    return resultBuffer.toString()
}

fun View.expand() {
    val v= this
    val matchParentMeasureSpec = makeMeasureSpec((v.parent as View).width, EXACTLY)
    val wrapContentMeasureSpec = makeMeasureSpec(0, UNSPECIFIED)
    v.measure(matchParentMeasureSpec, wrapContentMeasureSpec)
    val targetHeight = v.measuredHeight

    // Older versions of android (pre API 21) cancel animations for views with a height of 0.
    v.layoutParams.height = 1
    v.visibility = View.VISIBLE
    val a = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            v.layoutParams.height = if (interpolatedTime == 1f)
                ViewGroup.LayoutParams.WRAP_CONTENT
            else
                (targetHeight * interpolatedTime).toInt()
            v.requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    // Expansion speed of 1dp/ms
    a.duration = (targetHeight / v.context.resources.displayMetrics.density).toLong()
    v.startAnimation(a)
}

fun View.collapse() {
    val v = this
    val initialHeight = v.measuredHeight

    val a = object : Animation() {
        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            if (interpolatedTime == 1f) {
                v.visibility = View.GONE
            } else {
                v.layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                v.requestLayout()
            }
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    // Collapse speed of 1dp/ms
    a.duration = (initialHeight / v.context.resources.displayMetrics.density).toLong()
    v.startAnimation(a)
}
/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

object Extensions {

    fun getHashSHA256(source:String): String {
        val bytes = source.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    fun mergePaths(oldPath: String, newPath: String): String {
        return try {
            val oldUri = URI(oldPath)
            val resolved = oldUri.resolve(newPath)
            resolved.toString()
        } catch (e: URISyntaxException) {
            oldPath
        }
    }


}