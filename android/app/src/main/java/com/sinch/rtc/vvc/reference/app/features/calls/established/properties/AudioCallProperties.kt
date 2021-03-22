package com.sinch.rtc.vvc.reference.app.features.calls.established.properties

import com.sinch.android.rtc.AudioController

data class AudioCallProperties(
    val isMuted: Boolean,
    val isSpeakerOn: Boolean,
    val isAudioRoutingEnabled: Boolean
) {
    constructor(audioController: AudioController) :
            this(
                isMuted = audioController.isMute,
                isSpeakerOn = audioController.isSpeakerOn,
                isAudioRoutingEnabled = audioController.isAutomaticAudioRoutingEnabled
            )
}
