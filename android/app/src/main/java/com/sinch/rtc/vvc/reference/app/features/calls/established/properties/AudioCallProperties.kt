package com.sinch.rtc.vvc.reference.app.features.calls.established.properties

import com.sinch.android.rtc.AudioController
import com.sinch.rtc.vvc.reference.app.domain.calls.AudioState

data class AudioCallProperties(
    val isMuted: Boolean,
    val audioState: AudioState
) {
    constructor(audioController: AudioController, audioState: AudioState) :
            this(
                isMuted = audioController.isMute,
                audioState = audioState
            )
}
