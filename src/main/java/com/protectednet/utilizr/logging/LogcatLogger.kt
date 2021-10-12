package com.protectednet.utilizr.logging

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread


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
        val formatter = SimpleDateFormat("dd_MM_yyyy", Locale.getDefault())
        val date = formatter.format(Date(System.currentTimeMillis()))
        // create app folder
        if (!appDirectory.exists()) {
            appDirectory.mkdir()
        }
        // create log folder
        if (!logDirectory.exists()) {
            logDirectory.mkdir()
        }
        //do one file per day
        logFile = File(logDirectory.absolutePath, "logcat$date.txt")
        flushLogs()
    }

    /**
    This method should be called at intervals in-case the logs are heavy and taking up spaces.
    A 2-hourly interval is advised
     */
    fun flushLogs() {//logs can grow quite heave so clear regularly
        if (logFile == null) return
        if (!logDirectory.exists())
            return
        val files = logDirectory.listFiles()
        if (files != null && files.isNotEmpty())
            for (f in files) {
                try {
                    if (f.path != logFile?.path
                        || System.currentTimeMillis() - f.lastModified() > 6 * 60 * 60 * 1000
                        || f.length() > 10000000L
                    )
                        f.delete()
                } catch (e: Exception) {
                    Log.e("DeleteLog", e.message ?: "")
                }
            }
    }

    fun start() {
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