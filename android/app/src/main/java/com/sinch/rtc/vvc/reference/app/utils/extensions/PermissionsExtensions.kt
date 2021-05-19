package com.sinch.rtc.vvc.reference.app.utils.extensions

import android.Manifest


typealias PermissionRequestResult = Map<String, Boolean>

val PermissionRequestResult.areAllPermissionsGranted: Boolean
    get() =
        values.all { isGranted -> isGranted }

val PermissionRequestResult.areAudioPermissionsGranted: Boolean
    get() = this[Manifest.permission.RECORD_AUDIO] ?: false
