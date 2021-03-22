package com.sinch.rtc.vvc.reference.app.utils.extensions

import com.sinch.android.rtc.AudioController

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
        enableAutomaticAudioRouting(true, AudioController.UseSpeakerphone.SPEAKERPHONE_AUTO)
    } else {
        disableAutomaticAudioRouting()
    }
}