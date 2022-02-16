package com.protectednet.utilizr.logging

import android.os.Build
import android.os.Environment
import android.util.Log
import com.protectednet.utilizr.BuildConfig
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*


object LogcatLogger {

    const val TAG = "LogcatLogger"

    /*How often logcat should dump to file*/
    var isLogging = false
    lateinit var logDirectory: File
    private var process: Process? = null
    private var logFile: File? = null
    var flushInitiated = false

    /* Checks if external storage is available for read and write */
    fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }

    /* Checks if external storage is available to at least read */
    fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
    }

    fun init(
        filesDir: String,
        folder: String
    ) {
//            val appDirectory = File(Environment.getExternalStorageDirectory().absolutePath)
        val appDirectory = File(filesDir)
        logDirectory = File("$appDirectory/$folder")

        // create app folder
        if (!appDirectory.exists()) {
            appDirectory.mkdir()
        }
        // create log folder
        if (!logDirectory.exists()) {
            logDirectory.mkdir()
        }
        setLogFileName()
        flushLogs()
    }

    private fun setLogFileName(){
        val formatter = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
        val date = formatter.format(Date(System.currentTimeMillis()))
        //do one file per day
        logFile = File(logDirectory.absolutePath, "logcat$date.txt")
    }


    /**
    This method should be called at intervals in-case the logs are heavy and taking up space.
     A 2-hourly interval is advised
     */
    fun flushLogs() {//logs can grow quite heavy so clear regularly
        if (logFile == null) return
        if (!logDirectory.exists())
            return
        val files = logDirectory.listFiles()
        Log.d("flushLogs", "found ${files.size} log files")
        val fileAgeThreshold = if (BuildConfig.DEBUG) 1 * 60 * 1000L else 6 * 60 * 60 * 1000L
        val fileSizeThreshold = if (BuildConfig.DEBUG) 400 * 1000L else 10 * 1000 * 1000L
        if (files != null && files.isNotEmpty())
            for (f in files) {
                try {//delete the file if..
                    if (f.path != logFile?.path // file is not today's log file or..
                        || System.currentTimeMillis() - f.lastModified() > fileAgeThreshold//over 6hrs
                        || f.length() > fileSizeThreshold //file is too big
                    )
                        f.delete()
                } catch (e: Exception) {
                    Log.e("DeleteLog", e.message ?: "")
                }
            }
    }

    fun start() {
        setLogFileName()
        // clear the previous logcat and then write the new one to the file
        try {
            process = Runtime.getRuntime().exec("logcat -c")
            val sdk = Build.VERSION.SDK_INT
            val command =
                if (sdk < Build.VERSION_CODES.N) { // Versions below Nougat don't support the logcat --pid option. An alternative method has to be used.
                    "logcat -v threadtime -f ${logFile?.absolutePath} *:D | grep ${android.os.Process.myPid()}" // threadtime option used to make it as close as possible to the logs on newer devices.
                } else { // Nougat and above support the --pid option
                    "logcat -f ${logFile?.absolutePath} --pid ${android.os.Process.myPid()} *:D"
                }
            process = Runtime.getRuntime().exec(command)
            isLogging = true
        } catch (e: IOException) {
            e.printStackTrace()
            isLogging = false
        }
    }

    fun stop() {
        process?.destroy()
        process = null
    }

}