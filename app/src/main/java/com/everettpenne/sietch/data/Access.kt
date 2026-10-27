package com.everettpenne.sietch.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings as AndroidSettings

/** What the user has let Sietch see. Each level unlocks more, and every screen says what it is missing instead of failing quietly. */
data class AccessState(
    /** "All files access": every folder, and the Documents, Downloads and Apps categories. */
    val allFiles: Boolean,
    /** Some or all photos, videos and audio through the media library. */
    val media: Boolean,
) {
    /** Anything at all can be listed. */
    val any: Boolean get() = allFiles || media
}

object Access {
    val mediaPermissions = arrayOf(
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.READ_MEDIA_VIDEO,
        android.Manifest.permission.READ_MEDIA_AUDIO,
        android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )

    fun state(context: Context) = AccessState(
        allFiles = Environment.isExternalStorageManager(),
        // Any one of them counts: on Android 14+ the user may allow only some photos (VISUAL_USER_SELECTED), or photos but not audio. The lists then show
        // what the system lets Sietch see, which is what they should do.
        media = mediaPermissions.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED },
    )

    /** The system screen where the user turns "All files access" on for this app. */
    fun allFilesIntent(context: Context): Intent = Intent(AndroidSettings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
}
