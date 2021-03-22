package com.sinch.rtc.vvc.reference.app.utils.extensions

typealias PermissionRequestResult = Map<String, Boolean>

val PermissionRequestResult.areAllPermissionsGranted: Boolean
    get() =
        values.all { isGranted -> isGranted }
