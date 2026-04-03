package com.sinch.rtc.vvc.reference.app.utils.extensions

import android.content.Context
import android.media.AudioDeviceInfo
import com.sinch.rtc.vvc.reference.app.R

fun AudioDeviceInfo.friendlyName(context: Context): String = when (type) {
    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> context.getString(R.string.device_earpiece)
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> context.getString(R.string.device_speaker)
    AudioDeviceInfo.TYPE_WIRED_HEADSET -> context.getString(R.string.device_wired_headset)
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> context.getString(R.string.device_wired_headphones)
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> context.getString(R.string.device_bluetooth)
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> context.getString(R.string.device_bluetooth)
    AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> context.getString(R.string.device_usb)
    else -> productName.toString()
}
