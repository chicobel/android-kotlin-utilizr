package com.protectednet.utilizr

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi

sealed interface AppPermissible {
    val permissions: Set<String>
}

object AppPermissions {

    object Video : AppPermissible {
        val READ_PERMISSION =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO
            else Manifest.permission.READ_EXTERNAL_STORAGE
        val WRITE_PERMISSION =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_VIDEO
            else Manifest.permission.WRITE_EXTERNAL_STORAGE

        override val permissions: Set<String> = setOf(READ_PERMISSION, WRITE_PERMISSION)
    }

    object Photos : AppPermissible {
        val READ_PERMISSION =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
            else Manifest.permission.READ_EXTERNAL_STORAGE
        val WRITE_PERMISSION =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
            else Manifest.permission.WRITE_EXTERNAL_STORAGE

        override val permissions: Set<String> = buildSet {
            add(READ_PERMISSION)
            add(WRITE_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_MEDIA_LOCATION)
            }
        }
    }

    object Media : AppPermissible {
        override val permissions: Set<String> = Video.permissions + Photos.permissions
    }

    object Contacts : AppPermissible {
        val READ_PERMISSION = Manifest.permission.READ_CONTACTS
        val WRITE_PERMISSION = Manifest.permission.WRITE_CONTACTS

        val ACCOUNTS_PERMISSION = Manifest.permission.GET_ACCOUNTS // required on api 23 and below

        override val permissions: Set<String> =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) setOf(
                READ_PERMISSION,
                WRITE_PERMISSION,
                ACCOUNTS_PERMISSION
            ) else setOf(READ_PERMISSION, WRITE_PERMISSION)
    }


    object Calendar : AppPermissible {
        val READ_PERMISSION = Manifest.permission.READ_CALENDAR
        val WRITE_PERMISSION = Manifest.permission.WRITE_CALENDAR
        override val permissions: Set<String> = setOf(READ_PERMISSION, WRITE_PERMISSION)
    }

    @RequiresApi(33)
    object Notifications : AppPermissible {
        override val permissions: Set<String> = setOf(Manifest.permission.POST_NOTIFICATIONS)
    }

    @RequiresApi(31)
    object Alarms: AppPermissible {
        override val permissions: Set<String> = setOf(Manifest.permission.SCHEDULE_EXACT_ALARM)
    }
}