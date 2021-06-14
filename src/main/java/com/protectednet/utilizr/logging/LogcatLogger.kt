package com.protectednet.utilizr.logging

import android.content.Context
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
    lateinit var logDirectory:File
    private var process: Process? = null
    private lateinit var logFile: File

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

    fun init(filesDir: String, folder: String) {
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
//            if(!logFile.exists())
//                logFile.createNewFile()
        thread {
            if (!logDirectory.exists())
                return@thread
            val files = logDirectory.listFiles()
            if (files != null && files.isNotEmpty())
                for (f in files) {
//                    if (System.currentTimeMillis() - f.lastModified() > 6 * 60 * 60 * 1000)//delete if more than 6 hours old
//                        f.delete()
                    try {
                        if (f.path != logFile.path)
                            f.delete()
                    } catch (e: Exception) {
                        Log.e("DeleteLog",e.message?:"")
                    }
                }
        }
    }


    fun start() {
        // clear the previous logcat and then write the new one to the file
        try {
            process = Runtime.getRuntime().exec("logcat -c")
            val command = "logcat -f ${logFile.absolutePath} --pid ${android.os.Process.myPid()} *:D"
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