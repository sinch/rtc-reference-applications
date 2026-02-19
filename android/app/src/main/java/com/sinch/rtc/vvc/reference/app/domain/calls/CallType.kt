package com.sinch.rtc.vvc.reference.app.domain.calls

import android.Manifest
import android.content.Context
import android.os.Build
import com.sinch.rtc.vvc.reference.app.R

enum class CallType {
    AppToPhone,
    AppToAppAudio,
    AppToAppVideo,
    AppToSip,
    AppToConference,
}

fun CallType.newCallLabel(context: Context): String {
    val resource = when (this) {
        CallType.AppToPhone -> R.string.call_app_to_phone
        CallType.AppToAppAudio -> R.string.call_app_to_app_audio
        CallType.AppToAppVideo -> R.string.call_app_to_app_video
        CallType.AppToSip -> R.string.call_app_to_sip
        CallType.AppToConference -> R.string.call_app_to_conference
    }
    return context.getString(resource)

}

val CallType.requiredPermissions: List<String>
    get() {
        // Needed as Automatic Audio Routing (AAR) is enabled by default
        val bluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH
        return when (this) {
            CallType.AppToPhone,
            CallType.AppToAppAudio,
            CallType.AppToSip,
            CallType.AppToConference -> listOf(
                Manifest.permission.RECORD_AUDIO,
                bluetoothPermission
            )
            CallType.AppToAppVideo -> listOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                bluetoothPermission
            )
        }
    }