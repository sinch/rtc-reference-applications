package com.sinch.rtc.vvc.reference.app.utils.extensions

import android.hardware.Camera
import com.sinch.android.rtc.AudioController
import com.sinch.android.rtc.video.VideoController

fun AudioController.setSpeakerEnabled(isOn: Boolean) {
    if (isOn) {
        enableSpeaker()
    } else {
        disableSpeaker()
    }
}

fun AudioController.setMuted(isMuted: Boolean) {
    if (isMuted) {
        mute()
    } else {
        unmute()
    }
}

fun AudioController.setAutomaticRoutingEnabled(isEnabled: Boolean) {
    if (isEnabled) {
        enableAutomaticAudioRouting(
            AudioController.AudioRoutingConfig(
                AudioController.UseSpeakerphone.SPEAKERPHONE_AUTO,
                true
            )
        )
    } else {
        disableAutomaticAudioRouting()
    }
}

var VideoController.isFrontCameraUsedForCapture: Boolean
    get() = this.captureDevicePosition == Camera.CameraInfo.CAMERA_FACING_FRONT
    set(isUsed) {
        val newCapturePosition =
            if (isUsed) Camera.CameraInfo.CAMERA_FACING_FRONT else Camera.CameraInfo.CAMERA_FACING_BACK
        this.captureDevicePosition = newCapturePosition
    }